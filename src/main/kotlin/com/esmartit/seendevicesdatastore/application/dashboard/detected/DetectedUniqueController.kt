package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.services.QueryUniqueService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.interval
import java.time.Duration.ofSeconds

@RestController
@RequestMapping("/sensor-activity/v2")
class DetectedUniqueController(
        private val queryUniqueService: QueryUniqueService
) {

    @GetMapping(path = ["/new-devices-today"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyNewDevices(
            uniqueFilters: FilterUniqueRequest
    ): Flux<TotalDevices> {
        return interval(ofSeconds(0), ofSeconds(15)).flatMap { queryUniqueService.getNewDevicesToday(uniqueFilters) }
    }
}



