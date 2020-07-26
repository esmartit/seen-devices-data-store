package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class DetectedService(
    private val repository: DevicePositionReactiveRepository,
    private val clock: Clock
) {

    fun todayDetectedFlux(filters: QueryFilterRequest, someTimeAgo: () -> Instant): Flux<NowPresence> {
        return repository.findBySeenTimeGreaterThanEqual(someTimeAgo())
            .filter { filters.handle(it, clock) }
            .groupBy { it.seenTime }
            .flatMap { group -> groupByTime(group) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    fun nowDetectedFlux(someTimeAgo: () -> Instant): Flux<NowPresence> {
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