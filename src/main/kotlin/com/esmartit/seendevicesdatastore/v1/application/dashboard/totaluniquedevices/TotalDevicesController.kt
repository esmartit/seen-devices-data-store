package com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/sensor-activity")
class TotalDevicesController {

    @GetMapping(path = ["/unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        TODO()
    }
}