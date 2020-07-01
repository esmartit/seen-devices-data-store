package com.esmartit.seendevicesdatastore.application.dashboard.dailyregistered

import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class DailyRegisteredService(
    private val repository: RegisteredUserReactiveRepository,
    private val clock: Clock
) {

    fun getDailyRegisteredCount(zoneId: ZoneId): Flux<Long> {

        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()
        val earlyFlux = repository.findByInfoSeenTimeGreaterThanEqual(startOfDay)
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findByInfoSeenTimeGreaterThanEqual(startOfDay).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs, fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker)
    }
}