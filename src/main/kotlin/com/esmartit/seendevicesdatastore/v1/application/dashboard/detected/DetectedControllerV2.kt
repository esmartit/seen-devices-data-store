package com.esmartit.seendevicesdatastore.v1.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.services.ClockService
import com.esmartit.seendevicesdatastore.v2.application.ScanApiService
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiActivity
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity/v2")
class DetectedControllerV2(
    private val clock: ClockService,
    private val scanApiService: ScanApiService
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {
        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .flatMap {
                flux(zoneId, 30L).groupBy { it.seenTime }.flatMap { scanApiService.groupByTime(it) }
                    .sort { o1, o2 -> o1.time.compareTo(o2.time) }
                    .collectList()
            }
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .flatMap { flux(zoneId, 5L).groupBy { it.seenTime }.flatMap { scanApiService.groupByTime(it) }.last() }
            .map { it.inCount + it.limitCount + it.outCount }
            .map { DailyDevices(it, clock.now()) }
    }

    private fun flux(zoneId: ZoneId, minutes: Long): Flux<ScanApiActivity> {
        return scanApiService.filteredFluxByTime(
            startDateTimeFilter = clock.minutesAgo(zoneId, minutes).toInstant(),
            endDateTimeFilter = null,
            filters = FilterRequest(inRange = true)
        )
    }
}
