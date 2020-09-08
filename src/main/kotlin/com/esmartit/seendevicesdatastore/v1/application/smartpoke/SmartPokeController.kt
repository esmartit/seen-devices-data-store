package com.esmartit.seendevicesdatastore.v1.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.v1.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.v1.services.BigDataService
import com.esmartit.seendevicesdatastore.v1.services.DeviceWithPositionAndTimeGroup
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController(
    private val repository: DevicePositionReactiveRepository,
    private val clock: Clock,
    private val bigDataService: BigDataService
) {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: FilterRequest
    ): Flux<NowPresence> {

        val todayConnected =
            bigDataService.fluxByDateTime(startOfDay(requestFilters.timezone), null, requestFilters)
                .filter { it.isConnected }
                .let { bigDataService.presenceFlux(it) }
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

        return Flux.concat(earlyFlux, ticker).map {
            DailyDevices(
                it,
                clock.instant()
            )
        }
    }

    @GetMapping(path = ["/now-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)

        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap {
                bigDataService.fluxByDateTime(thirtyMinutesAgo(), null, null)
                    .let { bigDataService.presenceFlux(it) }.collectList()
            }
    }

    @GetMapping(path = ["/now-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap {
                bigDataService.fluxByDateTime(twoMinutesAgo(), null, null)
                    .filter { it.isConnected }
                    .let { bigDataService.presenceFlux(it) }.collectList()
            }
            .map {
                it.lastOrNull()?.run {
                    DailyDevices(
                        inCount + limitCount + outCount,
                        clock.instant()
                    )
                }
                    ?: DailyDevices(0, clock.instant())
            }
    }

    @GetMapping(path = ["/connected-registered"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getConnectedRegistered(requestFilters: FilterRequest): Flux<TimeAndCounters> {
        return bigDataService.filteredFluxGrouped(requestFilters)
            .flatMap { g ->
                g.scan(TimeAndDevices(g.key()!!)) { t: TimeAndDevices, u: DeviceWithPositionAndTimeGroup ->
                    t.changed = false
                    if (u.deviceWithPosition.isConnected) {
                        t.connected.computeIfAbsent(u.deviceWithPosition.clientMac) { true }
                        t.changed = true
                    }
                    val isRegistered = t.time == u.registeredTime
                    if (isRegistered) {
                        t.registered.computeIfAbsent(u.deviceWithPosition.clientMac) { true }
                        t.changed = true
                    }
                    t
                }.filter { it.changed }
                    .map {
                        TimeAndCounters(
                            it.time,
                            connected = it.connected.count(),
                            registered = it.registered.count()
                        )
                    }
            }
            .concatWith(Mono.just(TimeAndCounters(time = "", isLast = true)))
    }

    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()

    private fun filterNowConnected(device: DeviceWithPosition): Boolean {
        val ssid: String? = device.activity?.ssid
        val isNotNullOrEmpty = ssid?.isNotEmpty() ?: false
        return device.isWithinRange() && isNotNullOrEmpty
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
    val connected: Int = 0,
    val isLast: Boolean = false
)


data class TimeAndDevices(
    val time: String,
    val id: UUID = UUID.randomUUID(),
    val connected: MutableMap<String, Boolean> = mutableMapOf(),
    val registered: MutableMap<String, Boolean> = mutableMapOf(),
    var changed: Boolean = false
)