package com.esmartit.seendevicesdatastore.v1.application.dashboard.registered

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.v1.services.ClockService
import com.esmartit.seendevicesdatastore.v1.services.RegisteredService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity")
class RegisteredController(
    private val registeredService: RegisteredService,
    private val clock: ClockService
) {

    @GetMapping(path = ["/total-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
            .flatMap { registeredService.getTotalRegisteredCount() }
            .map { TotalDevices(it, clock.now()) }
    }

    @GetMapping(path = ["/daily-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {
        return registeredService.getDailyRegisteredCount(zoneId).map {
            DailyDevices(it, clock.now())
        }
    }

    @GetMapping(path = ["/now-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {
        return registeredService.getNowRegisteredCount(zoneId).map {
            DailyDevices(it, clock.now())
        }
    }
}