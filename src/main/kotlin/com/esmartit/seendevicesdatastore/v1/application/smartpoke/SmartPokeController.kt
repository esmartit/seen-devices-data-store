package com.esmartit.seendevicesdatastore.v1.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/smartpoke")
class SmartPokeController {

    @GetMapping(path = ["/today-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: FilterRequest
    ): Flux<NowPresence> {
        TODO()
    }

    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        TODO()
    }

    @GetMapping(path = ["/now-connected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        TODO()
    }

    @GetMapping(path = ["/now-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        TODO()
    }

    @GetMapping(path = ["/connected-registered"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getConnectedRegistered(requestFilters: FilterRequest): Flux<TimeAndCounters> {
        TODO()
    }
}


data class TimeAndCounters(
    val time: String,
    val id: UUID = UUID.randomUUID(),
    val registered: Int = 0,
    val connected: Int = 0,
    val isLast: Boolean = false
)


