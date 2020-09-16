package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface DailyScanApiRepository :
    MongoRepository<DailyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): DailyScanApiActivity?
}