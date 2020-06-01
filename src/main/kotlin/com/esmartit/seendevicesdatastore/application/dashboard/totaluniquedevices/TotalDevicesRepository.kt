package com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document(collection = "uniqueDevicesDetectedCount")
data class TotalDevices(val count: Long, val time: Instant)


interface TotalDevicesRepository : MongoRepository<TotalDevices, String>

interface TotalDevicesReactiveRepository :
    ReactiveMongoRepository<TotalDevices, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<TotalDevices>
}