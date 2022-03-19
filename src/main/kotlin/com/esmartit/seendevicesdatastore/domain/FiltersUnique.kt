package com.esmartit.seendevicesdatastore.domain

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterGroup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class FilterUniqueRequest(
        val timezone: ZoneId = ZoneId.of("UTC"),
        val startDate: String? = null,
        val endDate: String? = null,
        val startTime: String? = null,
        val endTime: String? = null,
        val groupBy: FilterGroup = FilterGroup.BY_DAY
) {

    private fun filter(param: Any?, param2: Any?): Boolean {
        return param?.let { it == param2 } ?: true
    }

    fun handle(event: UniqueDeviceYearly): Boolean {

        val seenTimeAtZone = event.seenTime.atZone(timezone)
        val detectedHour = seenTimeAtZone.hour
        val startHour = startTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 0
        val endHour = endTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 23

        return startDateTime?.isBefore(seenTimeAtZone) ?: true &&
                endDateTime?.plusDays(1)?.minusSeconds(1)?.isAfter(seenTimeAtZone) ?: true &&
                detectedHour >= startHour &&
                detectedHour <= endHour
    }

    val startDateTime: ZonedDateTime? by lazy {
        startDate.checkIsNotBlank()?.let { LocalDateTime.parse("$it${getTime(startTime)}").atZone(timezone) }
    }

    val endDateTime: ZonedDateTime? by lazy {
        endDate.checkIsNotBlank()?.let { LocalDateTime.parse("$it${getTime(endTime)}").atZone(timezone) }
    }

    private fun getTime(time: String?): String {
        return time.checkIsNotBlank()?.let { "T$it" } ?: "T00:00:00"
    }
}

private fun String?.checkIsNotBlank(): String? {
    return this?.takeIf { it.isNotBlank() }
}