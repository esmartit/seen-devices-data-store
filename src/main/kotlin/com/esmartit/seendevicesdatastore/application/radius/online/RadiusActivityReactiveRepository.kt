package com.esmartit.seendevicesdatastore.application.radius.online

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface RadiusActivityReactiveRepository : ReactiveMongoRepository<RadiusActivity, String>

@Repository
interface RadiusActivityRepository : MongoRepository<RadiusActivity, String> {
    fun findByInfoUsername(username: String): RadiusActivity?
    fun findByInfoCallingStationId(clientMac: String): RadiusActivity?
}