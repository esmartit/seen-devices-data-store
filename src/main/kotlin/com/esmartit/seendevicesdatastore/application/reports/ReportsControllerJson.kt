package com.esmartit.seendevicesdatastore.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.services.QueryService
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/reports")
class ReportsControllerJson(private val queryService: QueryService) {

    @GetMapping(path = ["/list"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): List<DeviceWithPositionRecord>? {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedReport(createContext)
            .map { DeviceWithPositionRecord(it) }
            .collectList().block()
    }

    @GetMapping(path = ["/listbytime"], produces = [APPLICATION_JSON_VALUE])
    fun getDetailedbyTime(
        filters: FilterRequest
    ): List<DeviceWithPositionRecord>? {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedbyTimeReport(createContext)
            .map { DeviceWithPositionRecord(it) }
            .collectList().block()
    }

    @GetMapping(path = ["/listbyuser"], produces = [APPLICATION_JSON_VALUE])
    fun getDetailedbyUser(
        filters: FilterRequest
    ): List<DeviceWithPositionRecord>? {

        val createContext = queryService.createContext(filters)
        return queryService.getDetailedbyUsername(createContext)
            .map { DeviceWithPositionRecord(it) }
            .collectList().block()
    }

    @GetMapping(path = ["/list-radius"], produces = [APPLICATION_JSON_VALUE])
    fun getRadiusDateiled(
        filters: FilterRequest
    ): List<RadiusDeviceRecord>? {

        val createContext = queryService.createContext(filters)
        return queryService.getRadiusDetailedReport(createContext)
            .map { RadiusDeviceRecord(it) }
            .collectList().block()
    }
}
