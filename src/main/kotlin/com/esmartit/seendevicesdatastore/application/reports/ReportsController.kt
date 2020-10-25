package com.esmartit.seendevicesdatastore.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.bson.Document
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/reports")
class ReportsController(
    private val scanApiService: ScanApiService,
    private val queryService: QueryService
) {

    @GetMapping(path = ["/list"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<Document> {

        val createContext = queryService.createContext(filters)

//        return scanApiService.filteredFlux(filters)
//            .filter { it.isInRange() }
//            .map { DeviceWithPositionRecord(it) }
//            .buffer(500)
//            .concatWith(Mono.just(listOf(DeviceWithPositionRecord(isLast = true))))
        return queryService.findScanApi(createContext)
    }
}

data class DeviceWithPositionRecord(val body: ScanApiActivity? = null, val isLast: Boolean = false)