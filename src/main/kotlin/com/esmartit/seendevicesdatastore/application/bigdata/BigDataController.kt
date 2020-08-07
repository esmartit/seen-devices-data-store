package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.application.dashboard.detected.QueryFilterRequest
import com.esmartit.seendevicesdatastore.consumer.SeenDevicesPositionConsumer
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID


@RestController
@RequestMapping("/bigdata")
class BigDataController(
    private val repository: DevicePositionReactiveRepository,
    private val clock: Clock
) {

    private val logger = LoggerFactory.getLogger(BigDataController::class.java)

    @GetMapping(path = ["/find-debug"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findDebug(
        requestFilters: QueryFilterRequest
    ): Flux<DeviceWithPosition> {

        val timeZone = requestFilters.timezone
        return flux(requestFilters, timeZone)
    }

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
        requestFilters: QueryFilterRequest
    ): Flux<BigDataPresence> {

        val timeZone = requestFilters.timezone
        val result = flux(requestFilters, timeZone)

        return when (requestFilters.groupBy) {
            FilterDateGroup.BY_DAY -> { it: DeviceWithPosition -> dayDate(it.seenTime.atZone(timeZone)) }
            FilterDateGroup.BY_WEEK -> { it: DeviceWithPosition -> weekDate(it.seenTime.atZone(timeZone)) }
            FilterDateGroup.BY_MONTH -> { it: DeviceWithPosition -> monthDate(it.seenTime.atZone(timeZone)) }
            FilterDateGroup.BY_YEAR -> { it: DeviceWithPosition -> yearDate(it.seenTime.atZone(timeZone)) }
        }.let { group -> result.groupBy(group) }.flatMap { group -> groupByTime(group) }
    }

    private fun flux(requestFilters: QueryFilterRequest, timeZone: ZoneId): Flux<DeviceWithPosition> {

        val getTime = { time: String? -> time?.takeIf { it.isNotBlank() }?.let { "T$it" } ?: "T00:00:00" }
        val startDate = requestFilters.startDate?.takeIf { it.isNotBlank() }
            ?.let { LocalDateTime.parse("$it${getTime(requestFilters.startTime)}").atZone(timeZone) }?.toInstant()
        val endDate = requestFilters.endDate?.takeIf { it.isNotBlank() }
            ?.let { LocalDateTime.parse("$it${getTime(requestFilters.endTime)}").atZone(timeZone) }?.toInstant()

        logger.info("$requestFilters - START= $startDate - END= $endDate")

        return when {
            startDate != null && endDate != null -> {
                repository.findBySeenTimeBetween(startDate, endDate)
            }
            startDate != null -> {
                repository.findBySeenTimeGreaterThanEqual(startDate)
            }
            endDate != null -> {
                repository.findBySeenTimeLessThanEqual(endDate)
            }
            else -> {
                repository.findAll()
            }
        }.filter { requestFilters.handle(it, clock) }

    }

    private fun groupByTime(group: GroupedFlux<String, DeviceWithPosition>): Flux<BigDataPresence> {
        return group.scan(BigDataPresence(UUID.randomUUID().toString(), group.key()!!)) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }

    private fun dayDate(time: ZonedDateTime): String {
        return time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }

    private fun weekDate(time: ZonedDateTime): String {
        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        val weekNumber = time[woy]
        return "${time.year}/$weekNumber"
    }

    private fun monthDate(time: ZonedDateTime): String {
        return time.format(DateTimeFormatter.ofPattern("yyyy/MM"))
    }

    private fun yearDate(time: ZonedDateTime): String {
        return time.format(DateTimeFormatter.ofPattern("yyyy"))
    }
}

data class BigDataPresence(
    val id: String? = null,
    val group: String,
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)
