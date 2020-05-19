package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCount
import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCountReactiveRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class DailyUniqueDevicesDetectedController(private val repository: DailyUniqueDevicesDetectedCountReactiveRepository) {

    @GetMapping(path = ["/daily-unique-devices-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<DailyUniqueDevicesDetectedCount> {
        val ticker = Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop()
        val counter = repository.findWithTailableCursorBy()
        return Flux.combineLatest(ticker, counter, BiFunction { _: Long, b: DailyUniqueDevicesDetectedCount -> b })
    }
}