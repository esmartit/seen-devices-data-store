package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.UniqueDevicesDetectedCount
import com.esmartit.seendevicesdatastore.repository.UniqueDevicesDetectedCountReactiveRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/sensor-activity")
class UniqueDevicesDetectedController(private val repository: UniqueDevicesDetectedCountReactiveRepository) {

    @GetMapping(path = ["/unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<UniqueDevicesDetectedCount> {
        return repository.findWithTailableCursorBy()
    }
}