package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.FilterDailyRequest
import com.esmartit.seendevicesdatastore.services.QueryDailyService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/v2/smartpoke")

class SmartPokeTotalController(
    private val queryDailyService: QueryDailyService

) {
    @GetMapping(path = ["/find-offline"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findTotalSmartPokeRaw(
            filtersDaily: FilterDailyRequest
    ): Flux<SmartPokeDailyDevice> {

        return queryDailyService.findTotalSmartPokeRaw(filtersDaily)
                .concatWith(Mono.just(SmartPokeDailyDevice( isLast = true)))
    }

}
data class SmartPokeDailyDevice (
        val spot: String = "",
        val sensor: String = "",
        val userName: String = "",
        val isLast: Boolean = false
)