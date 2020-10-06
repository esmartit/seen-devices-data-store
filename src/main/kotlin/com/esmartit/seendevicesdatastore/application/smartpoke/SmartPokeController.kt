package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.extra.math.sum
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController(
    private val clock: ClockService,
    private val commonService: CommonService,
    private val scanApiService: ScanApiService
) {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val todayDetected = commonService.todayFluxGrouped(filters.copy(isConnected = true))
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap { todayDetected.last(NowPresence(UUID.randomUUID().toString())) }
        return Flux.concat(todayDetected, latest)
    }

    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
        filters: FilterRequest
    ): Flux<DailyDevices> {
        val fifteenSeconds = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap {
                commonService.todayFluxGrouped(filters.copy(isConnected = true))
                    .map { it.inCount + it.limitCount + it.outCount }
                    .sum()
                    .map { DailyDevices(it, clock.now()) }
            }
    }

    @GetMapping(path = ["/now-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .flatMap {
                commonService.timeFlux(zoneId, 30L)
                    .filter { it.isConnected }
                    .groupBy { it.seenTime.truncatedTo(ChronoUnit.MINUTES) }
                    .flatMap { scanApiService.groupByTime(it) }
                    .sort { o1, o2 -> o1.time.compareTo(o2.time) }
                    .collectList()
            }
    }

    @GetMapping(path = ["/now-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .flatMap {
                commonService.timeFlux(zoneId, 5L)
                    .filter { it.isConnected }
                    .map { it.clientMac }
                    .distinct()
                    .count()
            }
            .map { DailyDevices(it, clock.now()) }
    }

    @GetMapping(path = ["/connected-registered"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getConnectedRegistered(requestFilters: FilterRequest): Flux<TimeAndCounters> {
        TODO()
    }
}


data class TimeAndCounters(
    val time: String,
    val id: UUID = UUID.randomUUID(),
    val registered: Int = 0,
    val connected: Int = 0,
    val isLast: Boolean = false
)


