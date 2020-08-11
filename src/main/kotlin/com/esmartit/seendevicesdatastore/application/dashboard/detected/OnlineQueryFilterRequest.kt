package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.radius.registered.Gender
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

sealed class QueryFilter(
    open val timezone: ZoneId = ZoneOffset.UTC,
    open val startTime: String? = null,
    open val endTime: String? = null,
    open val countryId: String? = null,
    open val stateId: String? = null,
    open val cityId: String? = null,
    open val spotId: String? = null,
    open val sensorId: String? = null,
    open val brands: List<String> = emptyList(),
    open val status: Position? = null,
    open val ageStart: String? = null,
    open val ageEnd: String? = null,
    open val gender: Gender? = null,
    open val zipCode: String? = null,
    open val memberShip: Boolean? = null
) {
    open fun handle(sensorAct: DeviceWithPosition, clock: Clock): Boolean {

        val ageStartFilter =
            ageStart?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age >= it } } ?: { true }
        val ageEndFilter = ageEnd?.takeIf { it.isNotBlank() }?.toInt()?.let { { age: Int -> age <= it } } ?: { true }

        val sensorCountry = sensorAct.activity?.accessPoint?.countryLocation?.countryId
        val sensorState = sensorAct.activity?.accessPoint?.countryLocation?.stateId
        val sensorCity = sensorAct.activity?.accessPoint?.countryLocation?.cityId
        val sensorSpot = sensorAct.activity?.accessPoint?.spotId
        val sensorName = sensorAct.activity?.accessPoint?.sensorName
        val sensorStatus = sensorAct.position
        val sensorGender = sensorAct.userInfo?.gender
        val sensorZipCode = sensorAct.userInfo?.zipCode
        val sensorMembership = sensorAct.userInfo?.memberShip
        val sensorAge = LocalDate.now(clock).year - (sensorAct.userInfo?.dateOfBirth?.year ?: 1900)

        return filter(countryId?.takeIf { it.isNotBlank() }, sensorCountry) &&
            filter(stateId?.takeIf { it.isNotBlank() }, sensorState) &&
            filter(cityId?.takeIf { it.isNotBlank() }, sensorCity) &&
            filter(spotId?.takeIf { it.isNotBlank() }, sensorSpot) &&
            filter(sensorId?.takeIf { it.isNotBlank() }, sensorName) &&
            filter(status, sensorStatus) &&
            filter(gender, sensorGender) &&
            filter(zipCode?.takeIf { it.isNotBlank() }, sensorZipCode) &&
            filter(memberShip, sensorMembership) &&
            ageStartFilter(sensorAge) &&
            ageEndFilter(sensorAge)
    }

    protected fun filter(param: Any?, param2: Any?): Boolean {
        return param?.let { it == param2 } ?: true
    }
}

data class OnlineQueryFilterRequest(
    override val timezone: ZoneId = ZoneOffset.UTC,
    override val startTime: String? = null,
    override val endTime: String? = null,
    override val countryId: String? = null,
    override val stateId: String? = null,
    override val cityId: String? = null,
    override val spotId: String? = null,
    override val sensorId: String? = null,
    override val brands: List<String> = emptyList(),
    override val status: Position? = null,
    override val ageStart: String? = null,
    override val ageEnd: String? = null,
    override val gender: Gender? = null,
    override val zipCode: String? = null,
    override val memberShip: Boolean? = null
) : QueryFilter(
    timezone,
    startTime,
    endTime,
    countryId,
    stateId,
    cityId,
    spotId,
    sensorId,
    brands,
    status,
    ageStart,
    ageEnd,
    gender,
    zipCode,
    memberShip
) {
    override fun handle(sensorAct: DeviceWithPosition, clock: Clock): Boolean {

        val startTimeParam = sensorAct.seenTime.atZone(timezone).hour
        val param = startTime?.takeIf { it.isNotBlank() }?.split(":")?.get(0)?.toInt() ?: 0
        return super.handle(sensorAct, clock) &&
            startTimeParam >= param
    }
}

data class BigDataQueryFilterRequest(
    override val timezone: ZoneId = ZoneOffset.UTC,
    override val startTime: String? = null,
    override val endTime: String? = null,
    override val countryId: String? = null,
    override val stateId: String? = null,
    override val cityId: String? = null,
    override val spotId: String? = null,
    override val sensorId: String? = null,
    override val brands: List<String> = emptyList(),
    override val status: Position? = null,
    override val ageStart: String? = null,
    override val ageEnd: String? = null,
    override val gender: Gender? = null,
    override val zipCode: String? = null,
    override val memberShip: Boolean? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val groupBy: FilterDateGroup = FilterDateGroup.BY_DAY
) : QueryFilter(
    timezone,
    startTime,
    endTime,
    countryId,
    stateId,
    cityId,
    spotId,
    sensorId,
    brands,
    status,
    ageStart,
    ageEnd,
    gender,
    zipCode,
    memberShip
)