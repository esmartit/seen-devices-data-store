package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.scanapi.hourly.ScanApiHourlyReactiveRepository
import com.esmartit.seendevicesdatastore.domain.FilterHourlyRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivityH
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
class ScanApiHourlyService(
        private val scanApiHourlyReactiveRepository: ScanApiHourlyReactiveRepository
) {

    fun filteredFluxByTime(
            startDateTimeFilter: Instant? = null,
            endDateTimeFilter: Instant? = null,
            filtersHourly: FilterHourlyRequest? = null
    ): Flux<ScanApiActivityH> {
        return when {
            startDateTimeFilter != null && endDateTimeFilter != null -> {
                scanApiHourlyReactiveRepository.findByDateAtZoneBetween(startDateTimeFilter, endDateTimeFilter)
            }
            startDateTimeFilter != null -> {
                scanApiHourlyReactiveRepository.findByDateAtZoneGreaterThanEqual(startDateTimeFilter)
            }
            endDateTimeFilter != null -> {
                scanApiHourlyReactiveRepository.findByDateAtZoneLessThanEqual(endDateTimeFilter)
            }
            else -> {
                scanApiHourlyReactiveRepository.findAll()
            }
        }.filter {
            filtersHourly?.handle(it) ?: true
        }
    }

    fun groupByTime(group: GroupedFlux<Instant, ScanApiActivityH>): Mono<NowPresence> {
        return group.reduce(
                NowPresence(
                        UUID.randomUUID().toString(),
                        group.key()!!.toString()
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