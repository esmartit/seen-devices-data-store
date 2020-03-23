package com.esmartit.seendevicesdatastore

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/sensor-activity")
class SensorActivityController(private val repository: DeviceStatRepository) {

    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<SensorActivity> {
        return repository.findAll()
    }
}