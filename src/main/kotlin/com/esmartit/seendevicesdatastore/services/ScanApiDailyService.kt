package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.scanapi.daily.ScanApiDailyReactiveRepository
import com.esmartit.seendevicesdatastore.domain.FilterDailyRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivityD
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
class ScanApiDailyService(
        private val scanApiDailyReactiveRepository: ScanApiDailyReactiveRepository
) {

    fun filteredFluxByTime(
            startDateTimeFilter: Instant? = null,
            endDateTimeFilter: Instant? = null,
            filtersDaily: FilterDailyRequest? = null
    ): Flux<ScanApiActivityD> {
        return when {
            startDateTimeFilter != null && endDateTimeFilter != null -> {
                scanApiDailyReactiveRepository.findByDateAtZoneBetween(startDateTimeFilter, endDateTimeFilter)
            }
            startDateTimeFilter != null -> {
                scanApiDailyReactiveRepository.findByDateAtZoneGreaterThanEqual(startDateTimeFilter)
            }
            endDateTimeFilter != null -> {
                scanApiDailyReactiveRepository.findByDateAtZoneLessThanEqual(endDateTimeFilter)
            }
            else -> {
                scanApiDailyReactiveRepository.findAll()
            }
        }.filter {
            filtersDaily?.handle(it) ?: true
        }
    }

    fun groupByTime(group: GroupedFlux<Instant, ScanApiActivityD>): Mono<NowPresence> {
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