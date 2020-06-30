package com.esmartit.seendevicesdatastore.application.registered

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface RegisteredDeviceReactiveRepository : ReactiveMongoRepository<RegisteredDevice, String>

@Repository
interface RegisteredDeviceRepository : MongoRepository<RegisteredDevice, String> {
    fun findByInfoUsername(clientMac: String): RegisteredDevice?
}