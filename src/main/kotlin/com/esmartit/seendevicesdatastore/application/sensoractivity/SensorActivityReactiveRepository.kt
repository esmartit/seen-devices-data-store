package com.esmartit.seendevicesdatastore.application.sensoractivity

import com.esmartit.seendevicesdatastore.domain.SensorActivity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface SensorActivityReactiveRepository : ReactiveMongoRepository<SensorActivity, String> {
    fun findByProcessed(processed: Boolean): Flux<SensorActivity>
}

@Repository
interface SensorActivityRepository : MongoRepository<SensorActivity, String>
