package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface HourlyScanApiRepository :
    MongoRepository<HourlyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): HourlyScanApiActivity?
}