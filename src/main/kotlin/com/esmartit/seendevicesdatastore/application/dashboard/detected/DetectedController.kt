package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.QueryService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.interval
import java.time.Duration.ofSeconds
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val queryService: QueryService
) {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity() =
        interval(ofSeconds(0), ofSeconds(15)).flatMap { queryService.getTotalDevicesAll() }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        filters: FilterRequest
    ): Flux<NowPresence> {
        return queryService.todayDetected(filters).concatWith(interval(ofSeconds(0), ofSeconds(15))
            .flatMap { queryService.todayDetected(filters).last(NowPresence(id = UUID.randomUUID().toString())) })
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        filters: FilterRequest
    ): Flux<TotalDevices> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryService.getTotalDevicesToday(filters) }
    }

    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyBrands(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = interval(ofSeconds(0), ofSeconds(15)).flatMap { queryService.getTodayDevicesGroupedByBrand(zoneId) }

    @GetMapping(path = ["/today-countries"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyCountries(
            @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = interval(ofSeconds(0), ofSeconds(15)).flatMap { queryService.getTodayDevicesGroupedByCountry(zoneId) }

}

enum class FilterDateGroup {
    BY_MINUTE, BY_HOUR, BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}

enum class FilterGroup {
    BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}