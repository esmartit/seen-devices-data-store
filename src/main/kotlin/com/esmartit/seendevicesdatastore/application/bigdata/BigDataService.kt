package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.application.dashboard.detected.OnlineQueryFilterRequest
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields
import java.util.Locale

@Service
class BigDataService(private val repository: DevicePositionReactiveRepository, private val clock: Clock) {

    fun filteredFlux(requestFilters: OnlineQueryFilterRequest): Flux<DeviceWithPosition> {

        val timeZone = requestFilters.timezone
        val getTime = { time: String? -> time?.takeIf { it.isNotBlank() }?.let { "T$it" } ?: "T00:00:00" }
        val startDate = requestFilters.startDate?.takeIf { it.isNotBlank() }
            ?.let { LocalDateTime.parse("$it${getTime(requestFilters.startTime)}").atZone(timeZone) }?.toInstant()
        val endDate = requestFilters.endDate?.takeIf { it.isNotBlank() }
            ?.let { LocalDateTime.parse("$it${getTime(requestFilters.endTime)}").atZone(timeZone) }?.toInstant()

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

    fun filteredFluxGrouped(requestFilters: OnlineQueryFilterRequest): Flux<GroupedFlux<String, DeviceWithPosition>> {
        val result = filteredFlux(requestFilters)
        val timeZone = requestFilters.timezone
        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        return when (requestFilters.groupBy) {
            FilterDateGroup.BY_DAY -> { it: DeviceWithPosition -> dayDate(it.seenTime.atZone(timeZone)) }
            FilterDateGroup.BY_WEEK -> { it: DeviceWithPosition -> weekDate(it.seenTime.atZone(timeZone), woy) }
            FilterDateGroup.BY_MONTH -> { it: DeviceWithPosition -> monthDate(it.seenTime.atZone(timeZone)) }
            FilterDateGroup.BY_YEAR -> { it: DeviceWithPosition -> yearDate(it.seenTime.atZone(timeZone)) }
        }.let { group -> result.groupBy(group) }
    }

    private fun dayDate(time: ZonedDateTime): String {
        return time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }

    private fun weekDate(time: ZonedDateTime, woy: TemporalField): String {
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