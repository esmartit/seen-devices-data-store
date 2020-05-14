package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountReactiveRepository
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountTailable
import com.esmartit.seendevicesdatastore.repository.UniqueDevicesDetectedCount
import com.esmartit.seendevicesdatastore.repository.UniqueDevicesDetectedCountReactiveRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/sensor-activity")
class HourlyDeviceCountController(private val repository: HourlyDeviceCountReactiveRepository) {

    @GetMapping(path = ["/hourly-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<HourlyDeviceCountTailable> {
        return repository.findWithTailableCursorBy()
    }
}