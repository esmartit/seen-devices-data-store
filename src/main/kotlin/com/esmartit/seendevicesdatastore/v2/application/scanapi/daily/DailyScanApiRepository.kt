package com.esmartit.seendevicesdatastore.v2.application.scanapi.daily

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface DailyScanApiRepository :
    MongoRepository<DailyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): DailyScanApiActivity?
}