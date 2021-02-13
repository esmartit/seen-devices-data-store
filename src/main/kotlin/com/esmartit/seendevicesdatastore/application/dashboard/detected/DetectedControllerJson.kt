package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/v3/sensor-activity")
class DetectedControllerJson(
    private val clock: ClockService,
    private val scanApiService: ScanApiService,
    private val queryService: QueryService,
    private val commonService: CommonService
) {

    @GetMapping(path = ["/total-detected-count"], produces = [APPLICATION_JSON_VALUE])
    fun getAllSensorActivity() = queryService.getTotalDevicesAll().blockLast()

    @GetMapping(path = ["/today-detected"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyDetected(filters: FilterRequest) = queryService.todayDetected(filters)

    @GetMapping(path = ["/today-detected-count"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyDetectedCount(filters: FilterRequest) = queryService.getTotalDevicesToday(filters).blockLast()

    @GetMapping(path = ["/today-brands"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyBrands(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = queryService.getTodayDevicesGroupedByBrand(zoneId).blockLast()

    @GetMapping(path = ["/today-countries"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyCountries(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = queryService.getTodayDevicesGroupedByCountry(zoneId).blockLast()

    @GetMapping(path = ["/now-detected"], produces = [APPLICATION_JSON_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): List<NowPresence>? {
        return commonService.timeFlux(zoneId, 30L)
            .groupBy { it.seenTime.truncatedTo(ChronoUnit.MINUTES) }
            .flatMap { scanApiService.groupByTime(it) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
            .collectList().block()
    }

    @GetMapping(path = ["/now-detected-count"], produces = [APPLICATION_JSON_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): DailyDevices? {

        return commonService.timeFlux(zoneId, 5L)
            .groupBy { it.seenTime.truncatedTo(ChronoUnit.MINUTES) }
            .flatMap { scanApiService.groupByTime(it) }
            .last(NowPresence())
            .map { it.inCount + it.limitCount + it.outCount }
            .map { DailyDevices(it, clock.now()) }
            .block()
    }
}
