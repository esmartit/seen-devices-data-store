package com.esmartit.seendevicesdatastore.application.uniquedevices

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant

@Document
data class UniqueDevice(
    val id: String? = null,
    @Indexed(unique = true)
    val clientMac: String,
    val created: Instant
)

@Repository
interface UniqueDeviceReactiveRepository : ReactiveMongoRepository<UniqueDevice, String> {
    fun findByClientMac(clientMac: String): Mono<UniqueDevice>
}

@Repository
interface UniqueDeviceRepository : MongoRepository<UniqueDevice, String> {
    fun findByClientMac(clientMac: String): UniqueDevice?
}

