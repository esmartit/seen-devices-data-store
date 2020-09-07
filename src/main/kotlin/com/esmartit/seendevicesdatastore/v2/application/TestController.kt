package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

@RestController
@RequestMapping("/test")
class TestController(
    private val clock: Clock,
    private val testService: TestService
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGet(@RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId): Flux<List<NowPresence>> {
        val thirtyMinutesAgo = { clock.instant().atZone(zoneId).minusMinutes(30L).toInstant() }
        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .map { testService.filteredFluxByTime(thirtyMinutesAgo()) }
            .flatMap { testService.presenceFlux(it).buffer() }
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetToday(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val zoneId = filters.timezone
        val startOfDay = { clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant() }
        TODO()
    }

    @GetMapping(path = ["/daily-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetDaily(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val dailyFilteredFlux = testService.dailyFilteredFlux(filters)
        return testService.presenceFlux(dailyFilteredFlux)
    }
}

