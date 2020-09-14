package com.esmartit.seendevicesdatastore.v1.application.dashboard.registered

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.v1.application.dashboard.registered.total.TotalRegistered
import com.esmartit.seendevicesdatastore.v1.application.dashboard.registered.total.TotalRegisteredReactiveRepository
import com.esmartit.seendevicesdatastore.v1.services.RegisteredService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class RegisteredController(
    private val registeredService: RegisteredService,
    private val repository: TotalRegisteredReactiveRepository
) {

    @GetMapping(path = ["/total-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalRegistered> {
        TODO()
    }

    @GetMapping(path = ["/daily-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {
        TODO()
    }

    @GetMapping(path = ["/now-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {
        TODO()
    }
}