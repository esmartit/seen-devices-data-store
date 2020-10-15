package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.DeviceAndPosition
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.CustomDateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.interval
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.sum
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
        return todayFlux(filters).concatWith(interval(ofSeconds(0), ofSeconds(15))
            .flatMap { todayFlux(filters).last(NowPresence(id = UUID.randomUUID().toString())) })
    }

    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
        filters: FilterRequest
    ): Flux<DailyDevices> {
        val fifteenSeconds = ofSeconds(15)
        return interval(ofSeconds(0), fifteenSeconds).onBackpressureDrop()
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

    private fun todayFlux(filters: FilterRequest): Flux<NowPresence> {
        return queryService.find(todayContext(filters))
            .groupBy { it.group }
            .flatMap { g -> groupByTime(g) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun todayContext(filters: FilterRequest): FilterContext {
        return FilterContext(
            filterRequest = filters.copy(groupBy = FilterDateGroup.BY_HOUR, isConnected = true),
            chain = listOf(
                CustomDateFilterBuilder(clock.startOfDay(filters.timezone).toInstant()),
                HourFilterBuilder(),
                LocationFilterBuilder(),
                BrandFilterBuilder(),
                StatusFilterBuilder(),
                UserInfoFilterBuilder()
            )
        )
    }

    fun groupByTime(group: GroupedFlux<String, DeviceAndPosition>): Mono<NowPresence> {
        return group.reduce(
            NowPresence(
                id = UUID.randomUUID().toString(),
                time = group.key()!!
            )
        ) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }
}


data class TimeAndCounters(
    val time: String,
    val id: UUID = UUID.randomUUID(),
    val registered: Int = 0,
    val connected: Int = 0,
    val isLast: Boolean = false
)


