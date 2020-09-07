package com.esmartit.seendevicesdatastore.v1.application.reports

import com.esmartit.seendevicesdatastore.v1.application.bigdata.BigDataService
import com.esmartit.seendevicesdatastore.v1.application.dashboard.detected.OnlineQueryFilterRequest
import com.esmartit.seendevicesdatastore.v1.repository.DeviceWithPosition
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/reports")
class ReportsController(private val bigDataService: BigDataService) {

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: OnlineQueryFilterRequest
    ): Flux<DeviceWithPositionRecord> {
        return bigDataService.filteredFlux(requestFilters)
            .map { DeviceWithPositionRecord(body = it) }
            .concatWith(
                Mono.just(
                    DeviceWithPositionRecord(isLast = true)
                )
            )
    }
}

data class DeviceWithPositionRecord(val body: DeviceWithPosition? = null, val isLast: Boolean = false)