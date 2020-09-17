package com.esmartit.seendevicesdatastore.domain

import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'clientMac':1, 'seenTime':1}", name = "daily_scan_api_activity_clientMac_seenTime_idx")
data class DailyScanApiActivity(
    val id: String? = null,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<HourlyScanApiActivity> = emptySet()
)
