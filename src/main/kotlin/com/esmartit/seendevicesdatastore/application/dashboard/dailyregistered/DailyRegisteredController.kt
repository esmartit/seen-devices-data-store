package com.esmartit.seendevicesdatastore.application.dashboard.dailyregistered

import com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices.DailyDevices
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Instant
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity")
class DailyRegisteredController(
    private val dailyRegisteredService: DailyRegisteredService
) {

    @GetMapping(path = ["/daily-registered-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {
        return dailyRegisteredService.getDailyRegisteredCount(zoneId)
            .map { DailyDevices(it, Instant.now()) }
    }
}