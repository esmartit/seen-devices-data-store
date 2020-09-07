package com.esmartit.seendevicesdatastore.v1.application.bigdata

import com.esmartit.seendevicesdatastore.v1.application.dashboard.detected.OnlineQueryFilterRequest
import com.esmartit.seendevicesdatastore.v1.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.v1.repository.Position
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.format.DateTimeFormatter
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

    @GetMapping(path = ["/average-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAveragePresence(
        requestFilters: OnlineQueryFilterRequest
    ) = bigDataService.filteredFlux(requestFilters)
        .groupBy { it.seenTime.atZone(requestFilters.timezone).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) }
        .flatMap { g ->
            g.scan(g.key()!! to mutableMapOf<String, MutableList<Int>>()) { t, u ->
                t.also {
                    it.second.computeIfAbsent(u.macAddress) { mutableListOf() }
                    it.second.computeIfPresent(u.macAddress) { k, l -> l.apply { add(u.countInAnHour) } }
                }
            }.map { it.first to it.second.mapValues { l -> l.value.average() }.values.average() }
        }
        .scan(mutableMapOf<String, Double>()) { t, u ->
            t.apply {
                this[u.first] = u.second
            }
        }.map { it.values.average() }
        .window(Duration.ofMillis(300))
        .flatMap { w -> w.last(0.0) }
        .map { AveragePresence(it) }
        .concatWith(Mono.just(AveragePresence(0.0, true)))

    private fun groupByTime(group: GroupedFlux<String, DeviceWithPositionAndTimeGroup>): Flux<BigDataPresence> {
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
            when (curr.deviceWithPosition.position) {
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

data class AveragePresence(val value: Double, val isLast: Boolean = false)
