package com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document
data class UniqueDevicesDetectedCount(val count: Long, val time: Instant)


interface UniqueDevicesDetectedCountRepository : MongoRepository<UniqueDevicesDetectedCount, String>

interface UniqueDevicesDetectedCountReactiveRepository :
    ReactiveMongoRepository<UniqueDevicesDetectedCount, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<UniqueDevicesDetectedCount>
}