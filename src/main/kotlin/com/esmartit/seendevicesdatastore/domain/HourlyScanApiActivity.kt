package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
    unique = true,
    def = "{'clientMac':1, 'seenTime':1}", name = "hourly_scan_api_activity_clientMac_seenTime_idx")
data class HourlyScanApiActivity(
    val id: String? = null,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<ScanApiActivity> = emptySet()
) {

    fun countInHour(filters: FilterRequest): Int {
        return activity.filter { filters.handle(it) }.count()
    }

    fun filter(filters: FilterRequest?): ScanApiActivity {
        return activity.filter { filters?.handle(it) ?: true }
            .maxBy { it.status.value }
            ?: ScanApiActivity(
                clientMac = clientMac,
                seenTime = seenTime,
                groupName = sensorSetting?.tags?.get("groupname"),
                hotspot = sensorSetting?.tags?.get("hotspot"),
                zone = sensorSetting?.tags?.get("zone")
            )
    }
}

