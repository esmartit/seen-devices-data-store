package com.esmartit.seendevicesdatastore.application.sensoractivity

import com.esmartit.seendevicesdatastore.domain.SensorActivity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SensorActivityReactiveRepository : ReactiveMongoRepository<SensorActivity, String>

@Repository
interface SensorActivityRepository : MongoRepository<SensorActivity, String> {
    fun findByDeviceClientMacAndDeviceSeenTime(clientMac: String, seenTime: Instant): List<SensorActivity>
}