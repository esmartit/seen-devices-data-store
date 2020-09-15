package com.esmartit.seendevicesdatastore.v1.services

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.ScanApiService
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiActivity
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZoneId

@Component
class CommonService(
    private val clock: ClockService,
    private val uniqueDeviceReactiveRepository: UniqueDeviceReactiveRepository,
    private val scanApiService: ScanApiService
) {

    fun allDevicesCount(): Mono<Long> {
        return uniqueDeviceReactiveRepository.count()
    }

    fun todayFlux(filters: FilterRequest): Flux<NowPresence> {
        val zoneId = filters.timezone
        return scanApiService.hourlyFilteredFlux(
            startDateTimeFilter = clock.startOfDay(zoneId).toInstant(),
            endDateTimeFilter = null,
            filters = filters
        ).groupBy { it.seenTime }.flatMap { scanApiService.groupByTime(it) }
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