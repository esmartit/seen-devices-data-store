package com.esmartit.seendevicesdatastore.v1.services

import com.esmartit.seendevicesdatastore.v1.application.radius.registered.RegisteredUserReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class RegisteredService(
    private val repository: RegisteredUserReactiveRepository,
    private val clock: Clock
) {

    fun getTotalRegisteredCount(): Mono<Long> {
        return repository.count()
    }

    fun getDailyRegisteredCount(zoneId: ZoneId): Flux<Long> {

        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()
        val earlyFlux = repository.findByInfoSeenTimeGreaterThanEqual(startOfDay)
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findByInfoSeenTimeGreaterThanEqual(startOfDay).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs, fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker)
    }

    fun getNowRegisteredCount(zoneId: ZoneId): Flux<Long> {

        val twoMinutesAgo = { clock.instant().atZone(zoneId).minusMinutes(2).toInstant() }
        val nowFlux = { repository.findByInfoSeenTimeGreaterThanEqual(twoMinutesAgo()).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        return Flux.interval(fifteenSecs).flatMap { nowFlux() }
    }
}