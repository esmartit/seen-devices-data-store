package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices.DailyDevices
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

//    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
//    fun getNowDetectedCount(
//        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
//    ): Flux<DeviceWithPosition> {
//
//        val twoMinutesAgo = { clock.instant().atZone(zoneId).minusMinutes(2).toInstant() }
//        val nowFlux = { repository.findBySeenTimeGreaterThanEqual(twoMinutesAgo()) }
//        val fifteenSecs = Duration.ofSeconds(15)
//        return Flux.interval(fifteenSecs).flatMap { nowFlux() }.map { DailyDevices(it, clock.instant()) }
//        return Flux.interval(fifteenSecs).flatMap { nowFlux() }
//    }
}