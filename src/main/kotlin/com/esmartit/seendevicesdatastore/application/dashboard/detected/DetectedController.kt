package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices.DailyDevices
import com.esmartit.seendevicesdatastore.application.dashboard.nowpresence.NowPresence
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val repository: DevicePositionReactiveRepository,
    private val clock: Clock
) {

    @GetMapping(path = ["/daily-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()
        val earlyFlux = repository.findBySeenTimeGreaterThanEqual(startOfDay)
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findBySeenTimeGreaterThanEqual(startOfDay).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker).map { DailyDevices(it, clock.instant()) }
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val nowFlux = { repository.findByLastUpdateGreaterThanEqual(twoMinutesAgo()).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(fifteenSecs).flatMap { nowFlux() }.map { DailyDevices(it, clock.instant()) }
    }

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<NowPresence> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(3).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val earlyFlux = someFlux(thirtyMinutesAgo)
        val fifteenSecs = Duration.ofSeconds(5)
        val currentFlux = Flux.interval(fifteenSecs, fifteenSecs).flatMap { someFlux(twoMinutesAgo) }
        return Flux.concat(earlyFlux, currentFlux)
    }

    private fun someFlux(someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findByLastUpdateGreaterThanEqual(someTimeAgo())
            .filter { it.isWithinRange() }
            .map { it.copy(lastUpdate = it.lastUpdate.truncatedTo(ChronoUnit.MINUTES)) }
            .groupBy { it.lastUpdate }
            .flatMap { group ->
                group.reduce(NowPresence(UUID.randomUUID().toString(), group.key()!!)) { acc, curr ->
                    when (curr.position) {
                        Position.IN -> acc.copy(inCount = acc.inCount + 1)
                        Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                        Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                        Position.NO_POSITION -> acc
                    }
                }
            }.sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }
}