package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.ZoneId

@Component
class CommonService(
    private val clock: ClockService,
    private val scanApiService: ScanApiService
) {

    fun todayFlux(filters: FilterRequest): Flux<ScanApiActivity> {
        val zoneId = filters.timezone
        return scanApiService.hourlyFilteredFlux(
            startDateTimeFilter = clock.startOfDay(zoneId).toInstant(),
            endDateTimeFilter = null,
            filters = filters
        )
    }

    fun todayFluxGrouped(filters: FilterRequest): Flux<NowPresence> {
        return todayFlux(filters)
            .groupBy { it.seenTime }.flatMap { scanApiService.groupByTime(it) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    fun timeFlux(zoneId: ZoneId, minutes: Long): Flux<ScanApiActivity> {
        return scanApiService.filteredFluxByTime(
            startDateTimeFilter = clock.minutesAgo(zoneId, minutes).toInstant(),
            endDateTimeFilter = null,
            filters = FilterRequest(inRange = true)
        )
    }
}