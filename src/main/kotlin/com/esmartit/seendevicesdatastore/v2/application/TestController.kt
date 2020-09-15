package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.services.ClockService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.ZoneId

@RestController
@RequestMapping("/test")
class TestController(
    private val clock: ClockService,
    private val scanApiService: ScanApiService
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGet(@RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId): Flux<List<NowPresence>> {

        return Flux.empty()
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetToday(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val zoneId = filters.timezone
        val startOfDay = clock.startOfDay(zoneId)
        return Flux.empty()
    }

    @GetMapping(path = ["/daily-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetDaily(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val dailyFilteredFlux = scanApiService.dailyFilteredFlux(filters)
        return scanApiService.scanByTime(dailyFilteredFlux)
    }
}

