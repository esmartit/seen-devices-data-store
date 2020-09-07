package com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.v2.application.scanapi.ScanApiActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
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
interface HourlyScanApiReactiveRepository : ReactiveMongoRepository<HourlyScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(time: Instant): Flux<HourlyScanApiActivity>
}

@Repository
interface HourlyScanApiRepository : MongoRepository<HourlyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): HourlyScanApiActivity?
}