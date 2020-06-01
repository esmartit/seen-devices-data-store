package com.esmartit.seendevicesdatastore.application.dashboard.nowpresence

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.function.BiFunction

@Service
class MinutePresenceService(private val repository: MinutePresenceCountRepository) {

    fun getPresenceAfter(someTimeAgo:Instant): Flux<MinutePresenceCountTailable> {

        val historyFlux = repository.findByTimeGreaterThanEqual(someTimeAgo)
            .groupBy { it.time }
            .flatMap { g -> g.reduce { _, u -> u } }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
            .filter { it.time.isBefore(getTwoMinutesAgo()) }

        val ticker = Flux.interval(Duration.ofSeconds(15)).onBackpressureDrop()
        val latest = repository.findWithTailableCursorBy()
            .scan(mutableMapOf(), ::scanNewEvents)
        val currentFlux =
            Flux.combineLatest(
                ticker,
                latest,
                BiFunction { _: Long, b: MutableMap<Int, MinutePresenceCountTailable> -> b })
                .map { getTwoMinutesAgoCount(it) }


        return Flux.concat(historyFlux, currentFlux)
    }

    private fun getTwoMinutesAgoCount(it: MutableMap<Int, MinutePresenceCountTailable>): MinutePresenceCountTailable {
        val twoMinutesAgo = getTwoMinutesAgo()
        return it[twoMinutesAgo.atZone(ZoneOffset.UTC).minute] ?: MinutePresenceCountTailable(
            time = twoMinutesAgo.truncatedTo(ChronoUnit.MINUTES),
            id = UUID.randomUUID().toString().replace("-", "").substring(0..22)
        )
    }

    private fun getTwoMinutesAgo() = Instant.now().minus(Duration.ofMinutes(2))

    private fun scanNewEvents(
        a: MutableMap<Int, MinutePresenceCountTailable>,
        c: MinutePresenceCountTailable
    ): MutableMap<Int, MinutePresenceCountTailable> {
        return a.apply { this[c.time.atZone(ZoneOffset.UTC).minute] = c }
    }
}
