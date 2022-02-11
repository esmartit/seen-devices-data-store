package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityD
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.ZoneId

@Component
class CommonDailyService(
        private val clock: ClockService,
        private val scanApiDailyService: ScanApiDailyService
) {

    fun timeDailyFlux(zoneId: ZoneId, minutes: Long): Flux<ScanApiActivityD> {
        return scanApiDailyService.filteredFluxByTime(
                startDateTimeFilter = clock.minutesAgo(zoneId, minutes).toInstant(),
                endDateTimeFilter = null
        )
    }
}