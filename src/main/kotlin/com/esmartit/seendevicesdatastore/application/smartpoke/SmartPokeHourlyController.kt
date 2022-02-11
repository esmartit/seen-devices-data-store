package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.FilterHourlyRequest
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.services.QueryHourlyService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*


@RestController
@RequestMapping("/smartpoke/v2")

class SmartPokeHourlyController(
        private val queryHourlyService: QueryHourlyService

) {
    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getHourlyConnected(
            filtersHourly: FilterHourlyRequest
    ): Flux<NowPresence> {
        val isConnected = filtersHourly.copy(isConnected = true)
        return queryHourlyService.todayDetected(isConnected)
                .concatWith(Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
                        .flatMap {
                            queryHourlyService.todayDetected(isConnected).last(NowPresence(id = UUID.randomUUID().toString()))
                        })
    }


    @GetMapping(path = ["/find-online"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findHourlySmartPokeRaw(
            filtersHourly: FilterHourlyRequest
    ): Flux<SmartPokeHourlyDevice> {

        return queryHourlyService.findHourlySmartPokeRaw(filtersHourly)
                .concatWith(Mono.just(SmartPokeHourlyDevice( isLast = true)))
    }

}

data class SmartPokeHourlyDevice (
        val spot: String = "",
        val sensor: String = "",
        val userName: String = "",
        val isLast: Boolean = false
)