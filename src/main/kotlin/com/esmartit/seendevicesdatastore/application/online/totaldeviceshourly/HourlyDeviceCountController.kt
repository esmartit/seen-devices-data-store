package com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly

import com.esmartit.seendevicesdatastore.service.CurrentDayHourlyCounter
import com.esmartit.seendevicesdatastore.service.HourlyDeltaCounter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity")
class HourlyDeviceCountController(
    private val currentDayHourlyCounter: CurrentDayHourlyCounter,
    private val hourlyDeltaCounter: HourlyDeltaCounter,
    private val repository: HourlyDeviceCountTailableRepository
) {

    @GetMapping(path = ["/hourly-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<HourlyDeviceCountTailable> {
        return repository.findWithTailableCursorBy()
    }

    @GetMapping(path = ["/hourly-device-presence-delta"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivityDelta(): Flux<HourlyDeviceCountTailable> {
        return hourlyDeltaCounter.getCounter()
    }

    @GetMapping(path = ["/today-hourly-device-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getHourlyPresence(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<HourlyDeviceCountTailable> {
        return currentDayHourlyCounter.getCounters(zoneId)
    }
}
