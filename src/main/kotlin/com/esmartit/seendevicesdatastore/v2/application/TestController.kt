package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.application.bigdata.BigDataPresence
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@RestController
@RequestMapping("/test")
class TestController(
    private val clock: ClockService,
    private val scanApiService: ScanApiService,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {



    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<Pair<String, List<Pair<Instant, Position>>>> {

        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        val timeZone = filters.timezone
        val timeFun: (ZonedDateTime) -> String = when (filters.groupBy) {
            FilterDateGroup.BY_DAY -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) }
            FilterDateGroup.BY_WEEK -> { time -> "${time.year}/${time[woy]}" }
            FilterDateGroup.BY_MONTH -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy/MM")) }
            FilterDateGroup.BY_YEAR -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy")) }
        }

        return dailyScanApiReactiveRepository.findAll()
            .map { it.clientMac to it.activity.flatMap { p->p.activity

                .map { f-> f.seenTime to f.status }
                .sortedBy { k->k.first }
            } }

    }









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

