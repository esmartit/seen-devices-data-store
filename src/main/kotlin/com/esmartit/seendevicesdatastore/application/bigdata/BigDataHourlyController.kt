package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.domain.FilterHourlyRequest
import com.esmartit.seendevicesdatastore.services.QueryHourlyService
import com.esmartit.seendevicesdatastore.v2.application.filter.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@RestController
@RequestMapping("/bigdata/v2")
class BigDataHourlyController(
        private val queryHourlyService: QueryHourlyService
) {

    @GetMapping(path = ["/average-time"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAverageHourlyTime(
            hourlyFilters: FilterHourlyRequest
    ): Flux<AverageHourlyPresence> {

        return queryHourlyService.avgHourlyDwellTime(hourlyFilters).concatWith(Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
                .flatMap { queryHourlyService.avgHourlyDwellTime(hourlyFilters).concatWith(Mono.just(AverageHourlyPresence(isLast = true))) })

//        return queryHourlyService.avgHourlyDwellTime(hourlyFilters)
//                .concatWith(Mono.just(AverageHourlyPresence(isLast = true)))
    }
}

data class AverageHourlyPresence(val value: Double = 0.0, val isLast: Boolean = false)
//data class AverageHourlyPresence(val id: String? = null, val value: Double = 0.0)

