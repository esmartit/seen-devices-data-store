package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.ZoneId

@Component
class CommonService(
    private val clock: ClockService,
    private val scanApiService: ScanApiService
) {

    fun timeFlux(zoneId: ZoneId, minutes: Long): Flux<ScanApiActivity> {
        return scanApiService.filteredFluxByTime(
            startDateTimeFilter = clock.minutesAgo(zoneId, minutes).toInstant(),
            endDateTimeFilter = null,
            filters = FilterRequest(inRange = true)
        )
    }
}