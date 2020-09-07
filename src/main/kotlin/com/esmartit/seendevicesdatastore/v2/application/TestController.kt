package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.v1.application.dashboard.detected.NowPresence
import com.esmartit.seendevicesdatastore.v1.repository.Position
import com.esmartit.seendevicesdatastore.v2.application.scanapi.ScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.ScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiReactiveRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/test")
class TestController(
    private val clock: Clock,
    private val scanApiReactiveRepository: ScanApiReactiveRepository,
    private val hourlyScanApiReactiveRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGet(@RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId): Flux<List<NowPresence>> {

        val thirtyMinutesAgo = { clock.instant().atZone(zoneId).minusMinutes(30L) }

        return Flux.interval(Duration.ofSeconds(0L), Duration.ofSeconds(15))
            .flatMap {
                scanApiReactiveRepository.findBySeenTimeGreaterThanEqual(thirtyMinutesAgo().toInstant())
                    .map { it.toPresence() }
                    .filter { it.position != Position.NO_POSITION }
                    .groupBy { it.time }
                    .flatMap { group -> groupByTime(group) }
                    .sort { o1, o2 -> o1.time.compareTo(o2.time) }
                    .buffer()
            }
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetToday(
        @RequestParam(
            name = "timezone",
            defaultValue = "UTC"
        ) zoneId: ZoneId
    ): Flux<HourlyScanApiActivity> {

        val startOfDay = { clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId) }
        return hourlyScanApiReactiveRepository.findBySeenTimeGreaterThanEqual(startOfDay().toInstant())
    }


    @GetMapping(path = ["/daily-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun testGetDaily(
        @RequestParam(
            name = "timezone",
            defaultValue = "UTC"
        ) zoneId: ZoneId
    ): Flux<DailyScanApiActivity> {

        val startOfDay = { clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId) }
        return dailyScanApiReactiveRepository.findBySeenTimeGreaterThanEqual(startOfDay().toInstant())
    }

    private fun groupByTime(group: GroupedFlux<Instant, DevicePresence>): Mono<NowPresence> {
        return group.reduce(NowPresence(UUID.randomUUID().toString(), group.key()!!)) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }
}

private fun ScanApiActivity.toPresence(): DevicePresence {
    val position = sensorSetting?.presence(rssi) ?: Position.NO_POSITION
    return DevicePresence(device.clientMac, seenTime, position)
}

data class DevicePresence(
    val clientMac: String,
    val time: Instant,
    val position: Position
)