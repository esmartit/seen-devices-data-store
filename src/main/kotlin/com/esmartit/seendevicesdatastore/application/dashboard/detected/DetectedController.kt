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
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
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

        val todayDetected = todayDetectedFlux2(requestFilters) { startOfDay(requestFilters.timezone) }
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(fifteenSeconds, fifteenSeconds).onBackpressureDrop()
            .flatMap { todayDetected.last() }

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

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<NowPresence> {

        val thirtyMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(30).toInstant().truncatedTo(ChronoUnit.MINUTES) }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSecs).flatMap { nowDetectedFlux(thirtyMinutesAgo) }
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val twoMinutesAgo =
            { clock.instant().atZone(zoneId).minusMinutes(2).toInstant().truncatedTo(ChronoUnit.MINUTES) }

        val earlyFlux = repository.findByLastUpdateGreaterThanEqual(twoMinutesAgo())
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findByLastUpdateGreaterThanEqual(twoMinutesAgo()).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker).map { DailyDevices(it, clock.instant()) }
    }

    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()

    private fun todayDetectedFlux(someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findBySeenTimeGreaterThanEqual(someTimeAgo())
            .filter { it.isWithinRange() }
            .groupBy { it.seenTime }
            .flatMap { group -> groupByTime(group) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun todayDetectedFlux2(filters: QueryFilterRequest, someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findBySeenTimeGreaterThanEqual(someTimeAgo())
            .filter { filters.handle(it) }
            .groupBy { it.seenTime }
            .flatMap { group -> groupByTime(group) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun nowDetectedFlux(someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findByLastUpdateGreaterThanEqual(someTimeAgo())
            .filter { it.isWithinRange() }
            .map { it.copy(lastUpdate = it.lastUpdate.truncatedTo(ChronoUnit.MINUTES)) }
            .groupBy { it.lastUpdate }
            .flatMap { group -> groupByTime(group) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun groupByTime(group: GroupedFlux<Instant, DeviceWithPosition>): Mono<NowPresence> {
        return group.reduce(NowPresence(UUID.randomUUID().toString(), group.key()!!)) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }
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
    val memberShip: Boolean? = null
) {
    fun handle(sensorAct: DeviceWithPosition): Boolean {

        val countryFilter = countryId?.let { { country: String? -> it == country } } ?: { true }
        val stateFilter = stateId?.let { { state: String? -> it == state } } ?: { true }
        val cityFilter = cityId?.let { { city: String? -> it == city } } ?: { true }
        val spotFilter = spotId?.let { { spot: String? -> it == spot } } ?: { true }
        val sensorFilter = sensorId?.let { { sensor: String? -> it == sensor } } ?: { true }
        val statusFilter = status?.let { { position: Position? -> it == position } } ?: { true }
        val ageStartFilter = ageStart?.toInt()?.let { { age: Int -> it >= age } } ?: { true }
        val ageEndFilter = ageEnd?.toInt()?.let { { age: Int -> it <= age } } ?: { true }
        val genderFilter = gender?.let { { genderParam: Gender? -> it == genderParam } } ?: { true }
        val zipCodeFilter = zipCode?.let { { zip: String? -> it == zip } } ?: { true }
        val memberFilter = memberShip?.let { { member: Boolean? -> it == member } } ?: { true }

        return countryFilter(sensorAct.activity?.accessPoint?.countryLocation?.countryId) &&
            stateFilter(sensorAct.activity?.accessPoint?.countryLocation?.stateId) &&
            cityFilter(sensorAct.activity?.accessPoint?.countryLocation?.cityId) &&
            spotFilter(sensorAct.activity?.accessPoint?.spotId) &&
            sensorFilter(sensorAct.activity?.accessPoint?.sensorName) &&
            statusFilter(sensorAct.position) &&
            ageStartFilter(sensorAct.userInfo?.dateOfBirth?.year ?: 0) &&
            ageEndFilter(sensorAct.userInfo?.dateOfBirth?.year ?: 0) &&
            genderFilter(sensorAct.userInfo?.gender) &&
            zipCodeFilter(sensorAct.userInfo?.zipCode) &&
            memberFilter(sensorAct.userInfo?.memberShip)
    }
}
