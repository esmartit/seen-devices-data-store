package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices.TotalDevices
import com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices.TotalDevicesReactiveRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.Gender
import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val repository: DevicePositionReactiveRepository,
    private val totalCountRepo: TotalDevicesReactiveRepository,
    private val service: DetectedService,
    private val clock: Clock
) {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        val ticker = Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop()
        val counter = totalCountRepo.findWithTailableCursorBy()
        return Flux.combineLatest(ticker, counter, BiFunction { _: Long, b: TotalDevices -> b })
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        requestFilters: QueryFilterRequest
    ): Flux<NowPresence> {

        val todayDetected =
            service.todayDetectedFlux({ requestFilters.handle(it, clock) }) { startOfDay(requestFilters.timezone) }
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap { todayDetected.last(NowPresence(UUID.randomUUID().toString())) }

        return Flux.concat(todayDetected, latest)
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val startOfDay = startOfDay(zoneId)
        val earlyFlux = repository.findBySeenTimeGreaterThanEqual(startOfDay)
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findBySeenTimeGreaterThanEqual(startOfDay).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker).map { DailyDevices(it, clock.instant()) }
    }

    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()
}

data class QueryFilterRequest(
    val timezone: ZoneId = UTC,
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
    fun handle(sensorAct: DeviceWithPosition, clock: Clock): Boolean {

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

    private fun filter(param: Any?, param2: Any?): Boolean {
        return param?.let { it == param2 } ?: true
    }
}

enum class FilterDateGroup {
    BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}