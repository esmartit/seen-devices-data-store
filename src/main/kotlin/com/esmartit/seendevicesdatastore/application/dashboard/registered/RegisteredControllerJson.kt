package com.esmartit.seendevicesdatastore.application.dashboard.registered

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.RegisteredService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId

@RestController
@RequestMapping("/v3/sensor-activity")
class RegisteredControllerJson(
    private val registeredService: RegisteredService,
    private val clock: ClockService
) {

    @GetMapping(path = ["/total-registered-count"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllSensorActivity(): TotalDevices? {
        return registeredService.getTotalRegisteredCount().defaultIfEmpty(0)
            .map { TotalDevices(it.toInt(), clock.now()) }
            .block()
    }

    @GetMapping(path = ["/daily-registered-count"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDailyRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): List<DailyDevices>? {
        return registeredService.getDailyRegisteredCount(zoneId).defaultIfEmpty(0)
            .map { DailyDevices(it, clock.now()) }.collectList().block()
    }

    @GetMapping(path = ["/now-registered-count"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getNowRegisteredCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): List<DailyDevices>? {
        return registeredService.getNowRegisteredCount(zoneId).defaultIfEmpty(0)
            .map { DailyDevices(it, clock.now()) }.collectList().block()
    }
}