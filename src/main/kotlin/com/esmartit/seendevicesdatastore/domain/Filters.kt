package com.esmartit.seendevicesdatastore.domain

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class FilterRequest(
    val timezone: ZoneId = ZoneId.of("UTC"),
    val startDate: String? = null,
    val endDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val presence: String? = null,
    val countryId: String? = null,
    val stateId: String? = null,
    val cityId: String? = null,
    val spotId: String? = null,
    val sensorId: String? = null,
    val zone: String? = null,
    val ssid: String? = null,
    val zipCodeId: String? = null,
    val brands: String? = null,
    val status: String? = null,
    val ageStart: String? = null,
    val ageEnd: String? = null,
    val gender: Gender? = null,
    val memberShip: Boolean? = null,
    val zipCode: String? = null,
    val groupBy: FilterDateGroup = FilterDateGroup.BY_DAY,
    val inRange: Boolean? = null,
    val isConnected: Boolean? = null,
    val startDateP: String? = null,
    val endDateP: String? = null
    ) {

    private fun filter(param: Any?, param2: Any?): Boolean {
        return param?.let { it == param2 } ?: true
    }

    fun handle(event: ScanApiActivity): Boolean {

        val seenTimeAtZone = event.seenTime.atZone(timezone)
        val sensorHour = seenTimeAtZone.hour
        val startHour = startTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 0
        val endHour = endTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 23

        val ageStartFilter =
            ageStart?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age >= it } } ?: { true }
        val ageEndFilter = ageEnd?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age <= it } } ?: { true }

        return startDateTime?.isBefore(seenTimeAtZone) ?: true &&
            endDateTime?.plusDays(1)?.minusSeconds(1)?.isAfter(seenTimeAtZone) ?: true &&
            sensorHour >= startHour &&
            sensorHour <= endHour &&
            ageStartFilter(event.age) && ageEndFilter(event.age) &&
            filter(countryId.checkIsNotBlank(), event.countryId) &&
            filter(stateId.checkIsNotBlank(), event.stateId) &&
            filter(cityId.checkIsNotBlank(), event.cityId) &&
            filter(zipCode.checkIsNotBlank(), event.zipCode) &&
            filter(sensorId.checkIsNotBlank(), event.sensorId) &&
            filter(zone.checkIsNotBlank(), event.zone) &&
            filter(ssid?.split(",")?.firstOrNull(),  event.ssid) &&
            filter(status?.split(",")?.firstOrNull(), event.status) &&
            filter(gender, event.gender) &&
            filter(memberShip, event.gender) &&
            filter(inRange, event.isInRange()) &&
            filter(isConnected, event.isConnected)
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