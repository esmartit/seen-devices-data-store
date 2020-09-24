package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.scanapi.hourly.HourlyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Component
class ScanApiService(
    private val scanApiReactiveRepository: ScanApiReactiveRepository,
    private val hourlyScanApiReactiveRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {

    fun filteredFlux(filters: FilterRequest): Flux<ScanApiActivity> {
        val startDateTimeFilter = getStartDateTime(filters)
        val endDateTimeFilter = getEndDateTime(filters)
        return filteredFluxByTime(startDateTimeFilter, endDateTimeFilter, filters)
    }

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

    fun hourlyFilteredFlux(
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
        }.map { it.filter(filters) }
            .filter { it.isInRange() }
    }

    fun dailyFilteredFlux(filters: FilterRequest): Flux<DailyScanApiActivity> {

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
        }
    }

    private fun getStartDateTime(filters: FilterRequest) =
        filters.startDateTime?.minusDays(1)?.toInstant()

    private fun getEndDateTime(filters: FilterRequest): Instant? {
        return filters.endDateTime?.plusDays(1)?.toInstant()
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
