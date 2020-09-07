package com.esmartit.seendevicesdatastore.v1.application.sensoractivity

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface SensorActivityReactiveRepository : ReactiveMongoRepository<SensorActivity, String> {

    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<SensorActivity>
}

@Repository
interface SensorActivityRepository : MongoRepository<SensorActivity, String> {
    fun findByDeviceMacAddressAndSeenTime(macAddress: String, seenTime: Instant): SensorActivity?
}