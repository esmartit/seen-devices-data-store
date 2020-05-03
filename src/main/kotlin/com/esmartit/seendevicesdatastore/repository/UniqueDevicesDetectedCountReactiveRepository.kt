package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux


interface UniqueDevicesDetectedCountRepository : MongoRepository<UniqueDevicesDetectedCount, String>
interface UniqueDevicesDetectedCountReactiveRepository : ReactiveMongoRepository<UniqueDevicesDetectedCount, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<UniqueDevicesDetectedCount>
}