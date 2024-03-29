package com.esmartit.seendevicesdatastore.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.services.QueryService
import org.bson.Document
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/reports")
class ReportsController(private val queryService: QueryService) {

    @GetMapping(path = ["/list"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<MutableList<DeviceWithPositionRecord>> {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedReport(createContext)
            .map { DeviceWithPositionRecord(it) }
            .buffer(500)
            .concatWith(Mono.just(listOf(DeviceWithPositionRecord(isLast = true))))
    }

    @GetMapping(path = ["/listbytime"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDetailedbyTime(
            filters: FilterRequest
    ): Flux<MutableList<DeviceWithPositionRecord>> {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedbyTimeReport(createContext)
            .map { DeviceWithPositionRecord(it) }
            .buffer(500)
            .concatWith(Mono.just(listOf(DeviceWithPositionRecord(isLast = true))))
    }

    @GetMapping(path = ["/list-daily"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyTotal(
            filters: FilterRequest
    ): Flux<MutableList<DailyDeviceWithPositionTotal>> {

        val createContext = queryService.createContext(filters)
        return queryService.getDailyDeviceTotal(createContext)
                .map { DailyDeviceWithPositionTotal(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(DailyDeviceWithPositionTotal(isLast = true))))
    }

    @GetMapping(path = ["/listbyuser"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDetailedbyUser(
            filters: FilterRequest
    ): Flux<MutableList<DeviceWithPositionRecord>> {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedbyUsername(createContext)
            .map { DeviceWithPositionRecord(it) }
            .buffer(500)
            .concatWith(Mono.just(listOf(DeviceWithPositionRecord(isLast = true))))
    }

    @GetMapping(path = ["/list-radius"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getRadiusDateiled(
            filters: FilterRequest
    ): Flux<MutableList<RadiusDeviceRecord>> {

        val createContext = queryService.createContext(filters)
        return queryService.getRadiusDetailedReport(createContext)
                .map { RadiusDeviceRecord(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(RadiusDeviceRecord(isLast = true))))
    }
}

data class DeviceWithPositionRecord(val body: Document? = null, val isLast: Boolean = false)

data class RadiusDeviceRecord(val body: Document? = null, val isLast: Boolean = false)

data class DailyDeviceWithPositionTotal(val body: Document? = null, val isLast: Boolean = false)
