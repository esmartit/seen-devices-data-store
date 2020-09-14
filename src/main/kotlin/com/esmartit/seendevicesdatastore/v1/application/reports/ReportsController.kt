package com.esmartit.seendevicesdatastore.v1.application.reports

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.FlatDevice
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/reports")
class ReportsController {

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: FilterRequest
    ): Flux<DeviceWithPositionRecord> {
        TODO()
    }
}

data class DeviceWithPositionRecord(val body: FlatDevice? = null, val isLast: Boolean = false)