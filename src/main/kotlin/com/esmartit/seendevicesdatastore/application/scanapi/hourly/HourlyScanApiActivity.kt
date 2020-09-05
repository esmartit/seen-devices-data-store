package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.application.scanapi.ScanApiActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Document
@CompoundIndex(def = "{'clientMac':1, 'seenTime':1}", name = "hourly_scan_api_activity_clientMac_seenTime_idx")
data class HourlyScanApiActivity(
    val id: String? = null,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<ScanApiActivity> = emptySet()
)

@Repository
interface HourlyScanApiReactiveRepository : ReactiveMongoRepository<HourlyScanApiActivity, String>

@Repository
interface HourlyScanApiRepository : MongoRepository<HourlyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): HourlyScanApiActivity?
}