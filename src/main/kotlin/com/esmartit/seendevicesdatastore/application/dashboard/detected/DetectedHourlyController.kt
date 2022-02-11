package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.FilterHourlyRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.services.QueryHourlyService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*

@RestController
@RequestMapping("/sensor-activity/v2")

class DetectedHourlyController(
        private val queryHourlyService: QueryHourlyService

) {
    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
            hourlyFilters: FilterHourlyRequest
    ): Flux<NowPresence> {
        return queryHourlyService.todayDetected(hourlyFilters).concatWith(Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
                .flatMap { queryHourlyService.todayDetected(hourlyFilters).last(NowPresence(id = UUID.randomUUID().toString())) })
    }

}