package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.application.dashboard.detected.OnlineQueryFilterRequest
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID


@RestController
@RequestMapping("/bigdata")
class BigDataController(
    private val bigDataService: BigDataService
) {

    @GetMapping(path = ["/find-debug"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findDebug(
        requestFilters: OnlineQueryFilterRequest
    ): Flux<DeviceWithPosition> {

        return bigDataService.filteredFlux(requestFilters)
    }

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: OnlineQueryFilterRequest
    ): Flux<BigDataPresence> {

        return bigDataService.filteredFluxGrouped(requestFilters)
            .flatMap(this::groupByTime)
            .window(Duration.ofMillis(300))
            .flatMap { w -> w.groupBy { it.id } }
            .flatMap { g -> g.last(BigDataPresence()) }
            .concatWith(Mono.just(BigDataPresence()))
    }

    private fun groupByTime(group: GroupedFlux<String, DeviceWithPosition>): Flux<BigDataPresence> {
        return group.scan(
            BigDataPresence(
                id = UUID.randomUUID().toString(),
                group = group.key()!!,
                inCount = 0,
                limitCount = 0,
                outCount = 0,
                isLast = false
            )
        ) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
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
