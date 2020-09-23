package com.esmartit.seendevicesdatastore.application.uniquedevices

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Document
data class UniqueDevice(
    val id: String
)

@Repository
interface UniqueDeviceReactiveRepository : ReactiveMongoRepository<UniqueDevice, String> {
//    fun findByClientMac(clientMac: String): Mono<UniqueDevice>
}

@Repository
interface UniqueDeviceRepository : MongoRepository<UniqueDevice, String> {
//    fun findByClientMac(clientMac: String): UniqueDevice?
}

