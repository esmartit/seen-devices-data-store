package com.esmartit.seendevicesdatastore.v1.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices.TotalDevices
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity")
class DetectedController {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        TODO()
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        requestFilters: FilterRequest
    ): Flux<NowPresence> {

        TODO()
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        TODO()
    }

    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyBrands(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<MutableList<BrandCount>> {
        TODO()
    }
}

data class BrandCount(val name: String, val value: Long)

enum class FilterDateGroup {
    BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}