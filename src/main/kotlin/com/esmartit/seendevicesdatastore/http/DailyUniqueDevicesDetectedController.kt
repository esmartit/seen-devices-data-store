package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCount
import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCountReactiveRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/sensor-activity")
class DailyUniqueDevicesDetectedController(private val repository: DailyUniqueDevicesDetectedCountReactiveRepository) {

    @GetMapping(path = ["/daily-unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<DailyUniqueDevicesDetectedCount> {
        return repository.findWithTailableCursorBy()
    }
}