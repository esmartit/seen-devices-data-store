package com.esmartit.seendevicesdatastore.v2.application.scanapi.daily

import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Document
@CompoundIndex(def = "{'clientMac':1, 'seenTime':1}", name = "daily_scan_api_activity_clientMac_seenTime_idx")
data class DailyScanApiActivity(
    val id: String? = null,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<HourlyScanApiActivity> = emptySet()
)

@Repository
interface DailyScanApiReactiveRepository : ReactiveMongoRepository<DailyScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(seenTime: Instant): Flux<DailyScanApiActivity>
}

@Repository
interface DailyScanApiRepository : MongoRepository<DailyScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): DailyScanApiActivity?
}