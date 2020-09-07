package com.esmartit.seendevicesdatastore.v1.services

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.FlatDevice
import com.esmartit.seendevicesdatastore.v1.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.v1.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.v1.repository.DeviceWithPosition
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields
import java.util.Locale

@Service
class BigDataService(private val repository: DevicePositionReactiveRepository, private val clock: Clock) {

    fun filteredFlux(requestFilters: FilterRequest): Flux<FlatDevice> {

        val startDate = getStartDateTime(requestFilters)
        val endDate = getEndDateTime(requestFilters)

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
        }.map { it.toFlatDevice(clock) }
            .filter { requestFilters.handle(it) }

    }

    fun filteredFluxGrouped(requestFilters: FilterRequest): Flux<GroupedFlux<String, DeviceWithPositionAndTimeGroup>> {
        val result = filteredFlux(requestFilters)
        val timeZone = requestFilters.timezone
        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        return when (requestFilters.groupBy) {
            FilterDateGroup.BY_DAY -> { it: Instant -> dayDate(it.atZone(timeZone)) }
            FilterDateGroup.BY_WEEK -> { it: Instant -> weekDate(it.atZone(timeZone), woy) }
            FilterDateGroup.BY_MONTH -> { it: Instant -> monthDate(it.atZone(timeZone)) }
            FilterDateGroup.BY_YEAR -> { it: Instant -> yearDate(it.atZone(timeZone)) }
        }.let { timeGroupFun ->
            result.map {
                DeviceWithPositionAndTimeGroup(
                    it,
                    timeGroupFun
                )
            }.groupBy { it.detectedTime }
        }
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

    private fun getStartDateTime(filters: FilterRequest) =
        filters.startDateTime?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()

    private fun getEndDateTime(filters: FilterRequest): Instant? {
        return filters.endDateTime?.toLocalDate()
            ?.plusDays(1)
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.minusSeconds(1)
            ?.toInstant()
    }
}

private fun DeviceWithPosition.toFlatDevice(clock: Clock): FlatDevice {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

data class DeviceWithPositionAndTimeGroup(
    val deviceWithPosition: FlatDevice,
    private val timeFun: (Instant) -> String
) {
    val detectedTime: String by lazy { timeFun.invoke(deviceWithPosition.seenTime) }

    val registeredTime: String by lazy { deviceWithPosition.registeredDate?.let { timeFun.invoke(it) } ?: "" }
}