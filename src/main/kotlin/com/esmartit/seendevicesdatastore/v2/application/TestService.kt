package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.FlatDevice
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.repository.Position
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiReactiveRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Component
class TestService(
    private val clock: Clock,
    private val scanApiReactiveRepository: ScanApiReactiveRepository,
    private val hourlyScanApiReactiveRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {

    fun filteredFluxByTime(
        startDateTimeFilter: Instant? = null,
        endDateTimeFilter: Instant? = null,
        filters: FilterRequest? = null
    ): Flux<FlatDevice> {
        return when {
            startDateTimeFilter != null && endDateTimeFilter != null -> {
                scanApiReactiveRepository.findBySeenTimeBetween(startDateTimeFilter, endDateTimeFilter)
            }
            startDateTimeFilter != null -> {
                scanApiReactiveRepository.findBySeenTimeGreaterThanEqual(startDateTimeFilter)
            }
            endDateTimeFilter != null -> {
                scanApiReactiveRepository.findBySeenTimeLessThanEqual(endDateTimeFilter)
            }
            else -> {
                scanApiReactiveRepository.findAll()
            }
        }.map { event ->
            event.toFlatDevice(clock).takeIf { filters?.handle(it) ?: true }
                ?: FlatDevice(
                    event.device.clientMac,
                    event.seenTime
                )
        }
    }

    fun filteredFlux(filters: FilterRequest): Flux<FlatDevice> {

        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)

        return filteredFluxByTime(startDateTimeFilter, endDateTimeFilter, filters)
    }

    fun hourlyFilteredFlux(filters: FilterRequest): Flux<FlatDevice> {

        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)

        return hourlyFilteredFlux(startDateTimeFilter, endDateTimeFilter, filters)
    }

    private fun hourlyFilteredFlux(
        startDateTimeFilter: Instant?,
        endDateTimeFilter: Instant?,
        filters: FilterRequest?
    ): Flux<FlatDevice> {
        return when {
            startDateTimeFilter != null && endDateTimeFilter != null -> {
                hourlyScanApiReactiveRepository.findBySeenTimeBetween(startDateTimeFilter, endDateTimeFilter)
            }
            startDateTimeFilter != null -> {
                hourlyScanApiReactiveRepository.findBySeenTimeGreaterThanEqual(startDateTimeFilter)
            }
            endDateTimeFilter != null -> {
                hourlyScanApiReactiveRepository.findBySeenTimeLessThanEqual(endDateTimeFilter)
            }
            else -> {
                hourlyScanApiReactiveRepository.findAll()
            }
        }.map { event ->
            event.activity
                .map { it.toFlatDevice(clock) }
                .filter { filters?.handle(it) ?: true }
                .maxBy { it.status }?.copy(seenTime = event.seenTime)
                ?: FlatDevice(
                    event.clientMac,
                    event.seenTime
                )
        }
    }

    fun dailyFilteredFlux(filters: FilterRequest): Flux<FlatDevice> {

        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)

        return when {
            startDateTimeFilter != null && endDateTimeFilter != null -> {
                dailyScanApiReactiveRepository.findBySeenTimeBetween(startDateTimeFilter, endDateTimeFilter)
            }
            startDateTimeFilter != null -> {
                dailyScanApiReactiveRepository.findBySeenTimeGreaterThanEqual(startDateTimeFilter)
            }
            endDateTimeFilter != null -> {
                dailyScanApiReactiveRepository.findBySeenTimeLessThanEqual(endDateTimeFilter)
            }
            else -> {
                dailyScanApiReactiveRepository.findAll()
            }
        }.map { event ->
            event.activity.flatMap { it.activity }
                .map { it.toFlatDevice(clock) }
                .filter { filters.handle(it) }
                .maxBy { it.status }?.copy(seenTime = event.seenTime)
                ?: FlatDevice(
                    event.clientMac,
                    event.seenTime
                )
        }
    }

    fun presenceFlux(someFlux: Flux<FlatDevice>): Flux<NowPresence> {
        return someFlux.filter { it.isInRange() }
            .window(Duration.ofMillis(150))
            .flatMap { w -> w.groupBy { it.seenTime }.flatMap { g -> groupByTime(g) } }
            .groupBy { it.time }
            .flatMap { g ->
                g.scan { t: NowPresence, u: NowPresence ->
                    t.copy(
                        inCount = t.inCount + u.inCount,
                        limitCount = t.limitCount + u.limitCount,
                        outCount = t.outCount + u.outCount
                    )
                }
            }
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

    private fun groupByTime(group: GroupedFlux<Instant, FlatDevice>): Mono<NowPresence> {
        return group.reduce(
            NowPresence(
                UUID.randomUUID().toString(),
                group.key()!!
            )
        ) { acc, curr ->
            when (curr.status) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }
}

private fun ScanApiActivity.toFlatDevice(clock: Clock): FlatDevice {
    return FlatDevice(
        clientMac = device.clientMac,
        seenTime = seenTime,
        age = userInfo?.dateOfBirth?.let { clock.instant().atZone(ZoneOffset.UTC).year - it.year } ?: 1900,
        gender = userInfo?.gender,
        brand = device.manufacturer,
        status = sensorSetting?.presence(rssi) ?: Position.NO_POSITION,
        memberShip = userInfo?.memberShip,
        spotId = sensorSetting?.tags?.get("spot_id"),
        sensorId = sensorSetting?.tags?.get("sensorname"),
        countryId = sensorSetting?.tags?.get("country"),
        stateId = sensorSetting?.tags?.get("state"),
        cityId = sensorSetting?.tags?.get("city"),
        zipCode = sensorSetting?.tags?.get("zipcode"),
        isConnected = !ssid.isNullOrBlank()
    )
}