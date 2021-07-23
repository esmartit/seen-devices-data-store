package com.esmartit.seendevicesdatastore.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterDailyRequest
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.services.QueryDailyService
import org.bson.Document
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/reports/v2")
class ReportsTotalController(private val queryDailyService: QueryDailyService) {

    @GetMapping(path = ["/list"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDevicePositionTime(
            dailyFilters: FilterDailyRequest
    ): Flux<MutableList<DeviceWithPositionTime>> {
        val dailyContext = queryDailyService.createContext(dailyFilters)
        return queryDailyService.getDetailedReportwithTime(dailyContext)
                .map { DeviceWithPositionTime(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(DeviceWithPositionTime(isLast = true))))
    }

    @GetMapping(path = ["/listbyuser"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetailedbyUser(
            dailyFilters: FilterDailyRequest
    ): Flux<MutableList<DeviceWithPositionReg>> {

        val dailyContext = queryDailyService.createContext(dailyFilters)
        return queryDailyService.getDailyDetailedbyUsername(dailyContext)
                .map { DeviceWithPositionReg(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(DeviceWithPositionReg(isLast = true))))
    }

}

data class DeviceWithPositionTime(val body: Document? = null, val isLast: Boolean = false)

data class DeviceWithPositionReg(val body: Document? = null, val isLast: Boolean = false)
