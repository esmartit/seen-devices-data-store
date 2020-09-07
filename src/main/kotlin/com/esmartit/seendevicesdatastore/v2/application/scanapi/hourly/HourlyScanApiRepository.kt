package com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface HourlyScanApiRepository :
    MongoRepository<HourlyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): HourlyScanApiActivity?
}