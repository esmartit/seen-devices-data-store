package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.sum
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID


@RestController
@RequestMapping("/bigdata")
class BigDataController(
    private val scanApiService: ScanApiService
) {

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Flux<BigDataPresence> {

        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        val timeZone = filters.timezone
        val timeFun: (ZonedDateTime) -> String = when (filters.groupBy) {
            BY_DAY -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) }
            BY_WEEK -> { time -> "${time.year}/${time[woy]}" }
            BY_MONTH -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy/MM")) }
            BY_YEAR -> { time -> time.format(DateTimeFormatter.ofPattern("yyyy")) }
        }

        return scanApiService.dailyFilteredFlux(filters)
            .map { timeFun(it.seenTime.atZone(timeZone)) to it }
            .window(Duration.ofMillis(150))
            .flatMap { w -> w.groupBy { it.first }.flatMap { g -> groupByTime(g) } }
            .groupBy { it.group }
            .flatMap { g ->
                g.scan { t: BigDataPresence, u: BigDataPresence ->
                    t.copy(
                        inCount = t.inCount + u.inCount,
                        limitCount = t.limitCount + u.limitCount,
                        outCount = t.outCount + u.outCount
                    )
                }
            }.concatWith(Mono.just(BigDataPresence()))
    }

    @GetMapping(path = ["/average-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAveragePresence(
        filters: FilterRequest
    ): Flux<AveragePresence> {
        return scanApiService.dailyFilteredFlux(filters)
            .window(Duration.ofMillis(300))
            .flatMap { w ->
                w.groupBy { it.seenTime }.flatMap { g -> g.sum { it.countInAnHour }.map { g.key() to it } }
            }
            .groupBy { it.first }
            .flatMap { g -> g.map { it.second.toDouble() }.scan { t, u -> (t + u) / 2 } }
            .map { AveragePresence(it) }
            .concatWith(Mono.just(AveragePresence(0.0, true)))
    }

    fun groupByTime(group: GroupedFlux<String, Pair<String, ScanApiActivity>>): Mono<BigDataPresence> {
        return group.reduce(
            BigDataPresence(
                id = UUID.randomUUID().toString(),
                group = group.key()!!,
                isLast = false
            )
        ) { acc, curr ->
            when (curr.second.status) {
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
