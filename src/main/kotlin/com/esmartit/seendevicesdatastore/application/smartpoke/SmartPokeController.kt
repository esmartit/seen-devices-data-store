package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.application.dashboard.detected.DailyDevices
import com.esmartit.seendevicesdatastore.application.dashboard.detected.DetectedService
import com.esmartit.seendevicesdatastore.application.dashboard.detected.NowPresence
import com.esmartit.seendevicesdatastore.application.dashboard.detected.QueryFilterRequest
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController(
    private val repository: DevicePositionReactiveRepository,
    private val service: DetectedService,
    private val clock: Clock
) {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: QueryFilterRequest
    ): Flux<NowPresence> {

        val todayConnected =
            service.todayDetectedFlux({
                filterTodayConnected(it, requestFilters)
            }) { startOfDay(requestFilters.timezone) }
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(fifteenSeconds, fifteenSeconds).onBackpressureDrop()
            .flatMap { todayConnected.last() }

        return Flux.concat(todayConnected, latest)
    }

    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val startOfDay = startOfDay(zoneId)
        val findByStartOfDay = repository
            .findBySeenTimeGreaterThanEqual(startOfDay)
            .filter(this::filterNowConnected)
        val earlyFlux = findByStartOfDay.scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { findByStartOfDay.count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker).map { DailyDevices(it, clock.instant()) }
    }

    @GetMapping(path = ["/now-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { service.nowDetectedFlux(this::filterNowConnected, thirtyMinutesAgo).collectList() }
    }

    @GetMapping(path = ["/now-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { service.nowDetectedFlux(this::filterNowConnected, twoMinutesAgo).collectList() }
            .map { it.last().run { DailyDevices(inCount + limitCount + outCount, clock.instant()) } }
    }

    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()

    private fun filterNowConnected(device: DeviceWithPosition): Boolean {
        val ssid: String? = device.activity?.ssid
        val isNotNullOrEmpty = ssid?.isNotEmpty() ?: false
        return device.isWithinRange() && isNotNullOrEmpty
    }

    private fun filterTodayConnected(device: DeviceWithPosition, filters: QueryFilterRequest): Boolean {
        val ssid: String? = device.activity?.ssid
        val isNotNullOrEmpty = ssid?.isNotEmpty() ?: false
        return device.isWithinRange() && isNotNullOrEmpty && filters.handle(device, clock)
    }
}
