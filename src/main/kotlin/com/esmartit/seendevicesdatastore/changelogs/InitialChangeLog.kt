package com.esmartit.seendevicesdatastore.changelogs

import com.github.mongobee.changeset.ChangeLog
import com.github.mongobee.changeset.ChangeSet
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document
import java.time.Instant

private const val UNIQUE_DEVICES_COUNT_COLLECTION = "uniqueDevicesDetectedCount"
private const val HOURLY_COUNT_TAILABLE = "hourlyDeviceCountTailable"
private const val DAILY_UNIQUE_COUNT_COLLECTION = "dailyUniqueDevicesDetectedCount"
private const val MINUTE_PRESENCE_TAILABLE = "minutePresenceCountTailable"

@ChangeLog(order = "001")
class InitialChangeLog {

    @ChangeSet(order = "001", id = "initialCollections", author = "gustavo.rodriguez@esmartit.es")
    fun initialCollections(db: MongoDatabase) {
        db.createCollection("sensorActivity")
        createCappedCollection(db, UNIQUE_DEVICES_COUNT_COLLECTION, 5000, 53248)
        insertFirstCount(db, UNIQUE_DEVICES_COUNT_COLLECTION)
    }

    @ChangeSet(order = "002", id = "hourlyDeviceCount", author = "gustavo.rodriguez@esmartit.es")
    fun hourlyDeviceCount(db: MongoDatabase) {
        db.createCollection("hourlyDeviceCount")
        createCappedCollection(db, HOURLY_COUNT_TAILABLE, 7_200 * 14, 102400)
        insertFirstPresence(db, HOURLY_COUNT_TAILABLE)
    }

    @ChangeSet(order = "003", id = DAILY_UNIQUE_COUNT_COLLECTION, author = "gustavo.rodriguez@esmartit.es")
    fun dailyUniqueDevicesDetectedCount(db: MongoDatabase) {
        createCappedCollection(db, DAILY_UNIQUE_COUNT_COLLECTION, 172_800 * 2, 102400)
        insertFirstCount(db, DAILY_UNIQUE_COUNT_COLLECTION)
    }

    @ChangeSet(order = "004", id = "adjustingSizes", author = "gustavo.rodriguez@esmartit.es")
    fun adjustingSizes(db: MongoDatabase) {
        db.getCollection(DAILY_UNIQUE_COUNT_COLLECTION).drop()
        db.getCollection(HOURLY_COUNT_TAILABLE).drop()
        db.getCollection(UNIQUE_DEVICES_COUNT_COLLECTION).drop()

        createCappedCollection(db, DAILY_UNIQUE_COUNT_COLLECTION, 100, 51200)
        createCappedCollection(db, HOURLY_COUNT_TAILABLE, 100, 51200)
        createCappedCollection(db, UNIQUE_DEVICES_COUNT_COLLECTION, 100, 51200)

        insertFirstCount(db, DAILY_UNIQUE_COUNT_COLLECTION)
        insertFirstCount(db, UNIQUE_DEVICES_COUNT_COLLECTION)
        insertFirstPresence(db, HOURLY_COUNT_TAILABLE)
    }

    @ChangeSet(order = "005", id = "minutePresenceCount", author = "gustavo.rodriguez@esmartit.es")
    fun minutePresenceCount(db: MongoDatabase) {
        createCappedCollection(db, MINUTE_PRESENCE_TAILABLE, 7_200, 31457280)
        insertFirstPresence(db, MINUTE_PRESENCE_TAILABLE)
    }

    @ChangeSet(order = "006", id = "minutePresenceCountFix", author = "gustavo.rodriguez@esmartit.es")
    fun minutePresenceCountFix(db: MongoDatabase) {
        db.getCollection(MINUTE_PRESENCE_TAILABLE).drop()
        createCappedCollection(db, MINUTE_PRESENCE_TAILABLE, 7_200, 31457280)
        insertFirstPresence(db, MINUTE_PRESENCE_TAILABLE)
    }

    private fun createCappedCollection(db: MongoDatabase, name: String, maxDocuments: Long, sizeInBytes: Long) {
        val options = CreateCollectionOptions().capped(true).maxDocuments(maxDocuments).sizeInBytes(sizeInBytes)
        db.createCollection(name, options)
    }

    private fun insertFirstCount(db: MongoDatabase, name: String) {
        val collection = db.getCollection(name)
        val logMessage = Document()
        logMessage.append("count", 0)
        logMessage.append("time", Instant.now())
        collection.insertOne(logMessage)
    }

    private fun insertFirstPresence(db: MongoDatabase, name: String) {
        val collection = db.getCollection(name)
        val logMessage = Document()
        logMessage.append("inCount", 0)
        logMessage.append("limitCount", 0)
        logMessage.append("outCount", 0)
        logMessage.append("time", Instant.now())
        collection.insertOne(logMessage)
    }
}
