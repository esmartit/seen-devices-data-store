package com.esmartit.seendevicesdatastore.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/reports")
class ReportsController(private val scanApiService: ScanApiService) {

    @GetMapping(path = ["/list"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<MutableList<DeviceWithPositionRecord>> {
        return scanApiService.filteredFlux(filters)
            .map { DeviceWithPositionRecord(it) }
            .buffer(100)
            .concatWith(Mono.just(listOf(DeviceWithPositionRecord(isLast = true))))
    }
}

data class DeviceWithPositionRecord(val body: ScanApiActivity? = null, val isLast: Boolean = false)