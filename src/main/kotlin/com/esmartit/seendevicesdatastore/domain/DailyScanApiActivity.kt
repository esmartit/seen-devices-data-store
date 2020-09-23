package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
    unique = true,
    def = "{'clientMac':1, 'seenTime':1}", name = "daily_scan_api_activity_clientMac_seenTime_idx"
)
data class DailyScanApiActivity(
    val id: String,
    val clientMac: String,
    val seenTime: Instant,
    val activity: Set<HourlyScanApiActivity> = emptySet()
) {
    fun sumInADay(filters: FilterRequest): Int {
        return activity.map { it.countInHour(filters) }.sum()
    }

    fun filter(filters: FilterRequest?): ScanApiActivity {
        return activity.flatMap { activity.map { it.filter(filters) } }.maxBy { it.status.value }
            ?: ScanApiActivity(
                id = "$clientMac;${seenTime.epochSecond}",
                clientMac = clientMac,
                seenTime = seenTime
            )
    }

    fun addActivity(hourlyScanApiActivity: HourlyScanApiActivity): DailyScanApiActivity {
        val newDayAct = activity.toMutableSet()
            .also { s -> s.removeIf { it.seenTime == hourlyScanApiActivity.seenTime } }
            .also { it.add(hourlyScanApiActivity) }
        return this.copy(activity = newDayAct)
    }
}

