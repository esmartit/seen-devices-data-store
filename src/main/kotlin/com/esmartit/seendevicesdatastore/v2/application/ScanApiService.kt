package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.minute.ScanApiReactiveRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Component
class ScanApiService(
    private val scanApiReactiveRepository: ScanApiReactiveRepository,
    private val hourlyScanApiReactiveRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {

    fun filteredFluxByTime(
        startDateTimeFilter: Instant? = null,
        endDateTimeFilter: Instant? = null,
        filters: FilterRequest? = null
    ): Flux<ScanApiActivity> {
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
        }.filter {
            filters?.handle(it) ?: true
        }
    }

    fun filteredFlux(filters: FilterRequest): Flux<ScanApiActivity> {

        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)

        return filteredFluxByTime(startDateTimeFilter, endDateTimeFilter, filters)
    }

    fun hourlyFilteredFlux(filters: FilterRequest): Flux<ScanApiActivity> {

        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)

        return hourlyFilteredFlux(startDateTimeFilter, endDateTimeFilter, filters)
    }

    private fun hourlyFilteredFlux(
        startDateTimeFilter: Instant?,
        endDateTimeFilter: Instant?,
        filters: FilterRequest?
    ): Flux<ScanApiActivity> {
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
                .filter { filters?.handle(it) ?: true }
                .maxBy { it.status }?.copy(seenTime = event.seenTime)
                ?: ScanApiActivity(
                    clientMac = event.clientMac,
                    seenTime = event.seenTime
                )
        }
    }

    fun dailyFilteredFlux(filters: FilterRequest): Flux<ScanApiActivity> {

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
                .filter { filters.handle(it) }
                .maxBy { it.status }?.copy(seenTime = event.seenTime)
                ?: ScanApiActivity(
                    clientMac = event.clientMac,
                    seenTime = event.seenTime
                )
        }
    }

    fun scanByTime(someFlux: Flux<ScanApiActivity>): Flux<NowPresence> {
        return someFlux
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

    fun groupByTime(group: GroupedFlux<Instant, ScanApiActivity>): Mono<NowPresence> {
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
