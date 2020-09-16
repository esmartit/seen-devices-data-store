package com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices

import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/sensor-activity")
class TotalDevicesController(
    private val commonService: CommonService,
    private val clockService: ClockService
) {

    @GetMapping(path = ["/unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
            .flatMap { commonService.allDevicesCount() }
            .map { TotalDevices(it, clockService.now()) }
    }
}