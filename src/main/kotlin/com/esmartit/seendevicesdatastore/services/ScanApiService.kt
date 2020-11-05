package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiReactiveRepository
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
    private val scanApiReactiveRepository: ScanApiReactiveRepository
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

    fun groupByTime(group: GroupedFlux<Instant, ScanApiActivity>): Mono<NowPresence> {
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
