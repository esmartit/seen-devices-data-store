package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.application.bigdata.BigDataService
import com.esmartit.seendevicesdatastore.application.bigdata.DeviceWithPositionAndTimeGroup
import com.esmartit.seendevicesdatastore.application.dashboard.detected.DailyDevices
import com.esmartit.seendevicesdatastore.application.dashboard.detected.DetectedService
import com.esmartit.seendevicesdatastore.application.dashboard.detected.NowPresence
import com.esmartit.seendevicesdatastore.application.dashboard.detected.OnlineQueryFilterRequest
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController(
    private val repository: DevicePositionReactiveRepository,
    private val service: DetectedService,
    private val clock: Clock,
    private val bigDataService: BigDataService
) {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: OnlineQueryFilterRequest
    ): Flux<NowPresence> {

        val todayConnected =
            service.todayDetectedFlux({
                filterTodayConnected(it, requestFilters)
            }) { startOfDay(requestFilters.timezone) }
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap { todayConnected.last(NowPresence(UUID.randomUUID().toString())) }

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
            .map {
                it.lastOrNull()?.run { DailyDevices(inCount + limitCount + outCount, clock.instant()) }
                    ?: DailyDevices(0, clock.instant())
            }
    }

    @GetMapping(path = ["/connected-registered"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getConnectedRegistered(requestFilters: OnlineQueryFilterRequest): Flux<TimeAndCounters> {
        return bigDataService.filteredFluxGrouped(requestFilters).flatMap(this::groupByTime)
    }

    private fun groupByTime(timeGroup: GroupedFlux<String, DeviceWithPositionAndTimeGroup>) =
        timeGroup.groupBy { it.deviceWithPosition.macAddress }
            .flatMap { byMacAddress -> groupByMacAddress(timeGroup.key()!!, byMacAddress) }
            .window(Duration.ofMillis(300))
            .flatMap { w -> w.reduce(TimeAndCounters(timeGroup.key()!!), this::reduceTime) }
            .scan { acc, curr ->
                acc.copy(
                    connected = acc.connected + curr.connected,
                    registered = acc.registered + curr.registered
                )
            }

    private fun groupByMacAddress(
        timeGroup: String,
        byMacAddress: GroupedFlux<String, DeviceWithPositionAndTimeGroup>
    ) = byMacAddress.reduce(TimeAndDevice(timeGroup, byMacAddress.key()!!), this::reduceDevice)

    private fun reduceDevice(acc: TimeAndDevice, curr: DeviceWithPositionAndTimeGroup): TimeAndDevice {
        val isConnected = acc.time == curr.detectedTime && curr.deviceWithPosition.isConnected()
        val isRegistered = acc.time == curr.registeredTime && curr.deviceWithPosition.isConnected()
        return acc.copy(connected = acc.connected || isConnected, registered = acc.registered || isRegistered)
    }

    private fun reduceTime(acc: TimeAndCounters, curr: TimeAndDevice): TimeAndCounters {
        val connectedCount = curr.connected.toInt()
        val registeredCount = curr.registered.toInt()
        return acc.copy(connected = acc.connected + connectedCount, registered = acc.registered + registeredCount)
    }

    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()

    private fun filterNowConnected(device: DeviceWithPosition): Boolean {
        val ssid: String? = device.activity?.ssid
        val isNotNullOrEmpty = ssid?.isNotEmpty() ?: false
        return device.isWithinRange() && isNotNullOrEmpty
    }

    private fun filterTodayConnected(device: DeviceWithPosition, filters: OnlineQueryFilterRequest): Boolean {
        val ssid: String? = device.activity?.ssid
        val isNotNullOrEmpty = ssid?.isNotEmpty() ?: false
        return device.isWithinRange() && isNotNullOrEmpty && filters.handle(device, clock)
    }
}

private fun Boolean.toInt() = if (this) 1 else 0

data class TimeAndDevice(
    val time: String,
    val macAddress: String,
    val id: UUID = UUID.randomUUID(),
    val registered: Boolean = false,
    val connected: Boolean = false
)

data class TimeAndCounters(
    val time: String,
    val id: UUID = UUID.randomUUID(),
    val registered: Int = 0,
    val connected: Int = 0
)