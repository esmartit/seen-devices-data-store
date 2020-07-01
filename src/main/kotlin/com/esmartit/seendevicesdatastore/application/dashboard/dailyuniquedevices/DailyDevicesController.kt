package com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices

import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
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

@Deprecated(replaceWith = ReplaceWith("DetectedController"), message = "")
@RestController
@RequestMapping("/sensor-activity")
class DailyDevicesController(
    private val rxRepo: DevicePositionReactiveRepository,
    private val clock: Clock
) {

    @GetMapping(path = ["/daily-unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()

        val counter = rxRepo.findBySeenTimeGreaterThanEqual(startOfDay)
            .groupBy { it.macAddress }
            .flatMap { g -> g.take(1).map { 1 } }
            .count()
            .map { DailyDevices(it, clock.instant()) }

        return Flux.interval(Duration.ofSeconds(1), Duration.ofSeconds(15)).onBackpressureDrop()
            .flatMap { counter }
    }
}