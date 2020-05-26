package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.MinutePresenceCountRepository
import com.esmartit.seendevicesdatastore.repository.MinutePresenceCountTailable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class MinutePresenceCountController(
    private val repository: MinutePresenceCountRepository
) {

    @GetMapping(path = ["/minute-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<MinutePresenceCountTailable> {

        val thirtyMinutesAgo = Instant.now().minus(Duration.ofMinutes(30))

        val historyFlux = repository.findByTimeGreaterThanEqual(thirtyMinutesAgo)
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
