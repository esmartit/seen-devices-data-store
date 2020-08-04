package com.esmartit.seendevicesdatastore.application.dashboard.detected

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
@RequestMapping("/sensor-activity/v2")
class DetectedControllerV2(
    private val service: DetectedService,
    private val clock: Clock
) {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { service.nowDetectedFlux({ it.isWithinRange() }, thirtyMinutesAgo).collectList() }
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs)
            .flatMap { service.nowDetectedFlux({ it.isWithinRange() }, twoMinutesAgo).collectList() }
            .map {
                it.lastOrNull()?.run { DailyDevices(inCount + limitCount + outCount, clock.instant()) }
                    ?: DailyDevices(0, clock.instant())
            }
    }
}
