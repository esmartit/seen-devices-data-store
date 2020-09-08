package com.esmartit.seendevicesdatastore.v1.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.v1.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.v1.repository.Position
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
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity/v2")
class DetectedControllerV2(
    private val clock: Clock,
    private val repository: DevicePositionReactiveRepository
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { nowDetectedFlux(thirtyMinutesAgo).collectList() }
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { nowDetectedFlux(twoMinutesAgo).collectList() }
            .map {
                it.lastOrNull()?.run {
                    DailyDevices(inCount + limitCount + outCount, clock.instant())
                }
                    ?: DailyDevices(0, clock.instant())
            }
    }

    private fun nowDetectedFlux(someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findByLastUpdateGreaterThanEqual(someTimeAgo())
            .filter { it.isWithinRange() }
            .map { it.copy(lastUpdate = it.lastUpdate.truncatedTo(ChronoUnit.MINUTES)) }
            .groupBy { it.lastUpdate }
            .flatMap { group -> groupByTime(group) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun groupByTime(group: GroupedFlux<Instant, DeviceWithPosition>): Mono<NowPresence> {
        return group.reduce(
            NowPresence(
                UUID.randomUUID().toString(),
                group.key()!!
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
