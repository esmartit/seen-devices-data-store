package com.esmartit.seendevicesdatastore.v1.application.bigdata

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.FlatDevice
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.UUID


@RestController
@RequestMapping("/bigdata")
class BigDataController {

    @GetMapping(path = ["/find-debug"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findDebug(
        requestFilters: FilterRequest
    ): Flux<FlatDevice> {

        TODO()
    }

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: FilterRequest
    ): Flux<BigDataPresence> {

        TODO()
    }

    @GetMapping(path = ["/average-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAveragePresence(
        requestFilters: FilterRequest
    ): Flux<AveragePresence> {
        TODO()
    }

}

data class BigDataPresence(
    val id: String = UUID.randomUUID().toString(),
    val group: String = "",
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0,
    val isLast: Boolean = true
)

data class AveragePresence(val value: Double, val isLast: Boolean = false)
