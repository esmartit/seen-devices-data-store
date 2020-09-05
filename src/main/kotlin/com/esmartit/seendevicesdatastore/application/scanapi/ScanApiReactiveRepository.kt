package com.esmartit.seendevicesdatastore.application.scanapi

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ScanApiReactiveRepository : ReactiveMongoRepository<ScanApiActivity, String>

@Repository
interface ScanApiRepository : MongoRepository<ScanApiActivity, String> {
    fun findByDeviceClientMacAndSeenTime(clientMac: String, seenTime: Instant): ScanApiActivity?
}