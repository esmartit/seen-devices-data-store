package com.esmartit.seendevicesdatastore

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface DeviceStatReactiveRepository : ReactiveMongoRepository<SensorActivity, String> {

    fun findBySeenTimeAfter(time: Instant): Flux<SensorActivity>

    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<SensorActivity>

    fun findBySeenTimeBetweenAndSeenTimeBetween(
        startDate: Instant,
        endDate: Instant,
        startHour: Instant,
        endHour: Instant
    ): Flux<SensorActivity>
}

@Repository
interface DeviceStatRepository : MongoRepository<SensorActivity, String>