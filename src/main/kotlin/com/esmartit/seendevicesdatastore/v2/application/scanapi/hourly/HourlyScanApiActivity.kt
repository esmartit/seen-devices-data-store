package com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'clientMac':1, 'seenTime':1}", name = "hourly_scan_api_activity_clientMac_seenTime_idx")
data class HourlyScanApiActivity(
    val id: String? = null,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<ScanApiActivity> = emptySet()
)

