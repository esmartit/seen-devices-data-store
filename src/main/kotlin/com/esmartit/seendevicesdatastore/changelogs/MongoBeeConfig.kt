package com.esmartit.seendevicesdatastore.changelogs

import com.github.mongobee.Mongobee
import com.github.mongobee.changeset.ChangeLog
import com.github.mongobee.changeset.ChangeSet
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant


@Configuration
class MongoBeeConfig {

    @Bean
    fun mongobee(@Value("\${spring.data.mongodb.uri}") mongoDbUri: String): Mongobee {
        val runner = Mongobee(mongoDbUri)
        runner.setDbName("smartpoke")
        runner.setChangeLogsScanPackage("com.esmartit.seendevicesdatastore.changelogs")
        return runner
    }
}

@ChangeLog(order = "001")
class InitialChangeLog {

    @ChangeSet(order = "001", id = "initialCollections", author = "gustavo.rodriguez@esmartit.es")
    fun initialCollections(db: MongoDatabase) {
        db.createCollection("sensorActivity")
        db.createCollection(
            "uniqueDevicesDetectedCount",
            CreateCollectionOptions().capped(true)
                .maxDocuments(5000)
                .sizeInBytes(53248)
        )
        val collection = db.getCollection("uniqueDevicesDetectedCount")
        val logMessage = Document()
        logMessage.append("count", 0)
        logMessage.append("time", Instant.now())
        collection.insertOne(logMessage)
    }

    @ChangeSet(order = "002", id = "hourlyDeviceCount", author = "gustavo.rodriguez@esmartit.es")
    fun hourlyDeviceCount(db: MongoDatabase) {
        db.createCollection("hourlyDeviceCount")
        db.createCollection(
            "hourlyDeviceCountTailable",
            CreateCollectionOptions().capped(true)
                .maxDocuments(7_200 * 14)
                .sizeInBytes(102400)
        )
        val collection = db.getCollection("hourlyDeviceCountTailable")
        val logMessage = Document()
        logMessage.append("inCount", 0)
        logMessage.append("limitCount", 0)
        logMessage.append("outCount", 0)
        logMessage.append("time", Instant.now())
        collection.insertOne(logMessage)
    }

    @ChangeSet(order = "003", id = "dailyUniqueDevicesDetectedCount", author = "gustavo.rodriguez@esmartit.es")
    fun dailyUniqueDevicesDetectedCount(db: MongoDatabase) {
        db.createCollection(
            "dailyUniqueDevicesDetectedCount",
            CreateCollectionOptions().capped(true)
                .maxDocuments(172_800 * 2)
                .sizeInBytes(102400)
        )
        val collection = db.getCollection("dailyUniqueDevicesDetectedCount")
        val logMessage = Document()
        logMessage.append("count", 0)
        logMessage.append("time", Instant.now())
        collection.insertOne(logMessage)
    }
}