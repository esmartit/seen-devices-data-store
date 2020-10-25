package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.interval
import java.time.Duration.ofSeconds
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController(
    private val clock: ClockService,
    private val commonService: CommonService,
    private val scanApiService: ScanApiService,
    private val queryService: QueryService
) {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val isConnected = filters.copy(isConnected = true)
        return queryService.todayDetected(isConnected)
            .concatWith(interval(ofSeconds(0), ofSeconds(15))
                .flatMap {
                    queryService.todayDetected(isConnected).last(NowPresence(id = UUID.randomUUID().toString()))
                })
    }

    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
        filters: FilterRequest
    ): Flux<TotalDevices> {
        val isConnected = filters.copy(isConnected = true)
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryService.getTotalDevicesToday(isConnected) }
    }

    @GetMapping(path = ["/now-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        return interval(ofSeconds(0L), ofSeconds(15))
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

        return interval(ofSeconds(0L), ofSeconds(15))
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


