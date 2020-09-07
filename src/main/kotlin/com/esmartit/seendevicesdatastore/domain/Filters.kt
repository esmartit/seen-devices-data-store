package com.esmartit.seendevicesdatastore.domain

import com.esmartit.seendevicesdatastore.v1.application.dashboard.detected.FilterDateGroup
import com.esmartit.seendevicesdatastore.v1.application.radius.registered.Gender
import com.esmartit.seendevicesdatastore.v1.repository.Position
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


data class FilterRequest(
    val timezone: ZoneId = ZoneOffset.UTC,
    val startTime: String? = null,
    val endTime: String? = null,
    val countryId: String? = null,
    val stateId: String? = null,
    val cityId: String? = null,
    val spotId: String? = null,
    val sensorId: String? = null,
    val brands: List<String> = emptyList(),
    val status: Position? = null,
    val ageStart: String? = null,
    val ageEnd: String? = null,
    val gender: Gender? = null,
    val zipCode: String? = null,
    val memberShip: Boolean? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val groupBy: FilterDateGroup = FilterDateGroup.BY_DAY
) {

    private fun filter(param: Any?, param2: Any?): Boolean {
        return param?.let { it == param2 } ?: true
    }

    fun handle(event: FlatDevice): Boolean {

        val sensorHour = event.seenTime.atZone(timezone).hour
        val startHour = startTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 0
        val endHour = endTime.checkIsNotBlank()?.split(":")?.get(0)?.toInt() ?: 23

        val ageStartFilter =
            ageStart?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age >= it } } ?: { true }
        val ageEndFilter = ageEnd?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age <= it } } ?: { true }

        return ageStartFilter(event.age) && ageEndFilter(event.age) &&
            filter(countryId.checkIsNotBlank(), event.countryId) &&
            filter(stateId.checkIsNotBlank(), event.stateId) &&
            filter(cityId.checkIsNotBlank(), event.cityId) &&
            filter(zipCode.checkIsNotBlank(), event.zipCode) &&
            filter(sensorId.checkIsNotBlank(), event.sensorId) &&
            filter(status, event.status) &&
            filter(gender, event.gender) &&
            filter(memberShip, event.gender) &&
            sensorHour >= startHour &&
            sensorHour <= endHour
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