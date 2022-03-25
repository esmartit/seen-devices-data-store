package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.services.QueryDailyService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.interval
import java.time.Duration.ofSeconds
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity/v2")
class DetectedTotalController(
        private val queryDailyService: QueryDailyService
) {

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
            dailyFilters: FilterDailyRequest
    ): Flux<TotalDevices> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTotalDevicesToday(dailyFilters) }
    }

//    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
//    fun getDailyBrands(
//            @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
//    ) = interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTodayDevicesGroupedByBrand(zoneId) }

    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyBrands(
            dailyFilters: FilterDailyRequest
    ): Flux<List<BrandCount>> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTodayDevicesGroupedByBrand(dailyFilters) }
    }


    @GetMapping(path = ["/today-zones"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyZones(
            dailyFilters: FilterDailyRequest
    ): Flux<List<ZoneCount>> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTodayDevicesGroupedByZone(dailyFilters) }
    }


//    @GetMapping(path = ["/today-countries"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
//    fun getDailyCountries(
//            @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
//    ) = interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTodayDevicesGroupedByCountry(zoneId) }

    @GetMapping(path = ["/today-countries"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyCountries(
            dailyFilters: FilterDailyRequest
    ): Flux<List<CountryCount>> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryDailyService.getTodayDevicesGroupedByCountry(dailyFilters) }
    }


}
