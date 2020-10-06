package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
    unique = true,
    def = "{'clientMac':1, 'seenTime':1}", name = "hourly_scan_api_activity_clientMac_seenTime_idx"
)
data class HourlyScanApiActivity(
    val id: String,
    val clientMac: String,
    @Indexed(name = "seenTime_idx")
    val seenTime: Instant,
    val activity: Set<ScanApiActivity> = emptySet()
) {

    fun countInHour(filters: FilterRequest): Int {
        return activity.filter { filters.handle(it) }.count()
    }

    fun filter(filters: FilterRequest?): ScanApiActivity {
        return activity.filter { filters?.handle(it) ?: true }
            .maxBy { it.status.value }
            ?.copy(seenTime = seenTime)
            ?: ScanApiActivity(
                id = "$clientMac;${seenTime.epochSecond}",
                clientMac = clientMac,
                seenTime = seenTime
            )
    }

    fun addActivity(event: ScanApiActivity): HourlyScanApiActivity {
        val newAct = activity.toMutableSet()
            .also { s -> s.removeIf { it.seenTime == event.seenTime } }
            .also { it.add(event) }
        return this.copy(activity = newAct)
    }
}

