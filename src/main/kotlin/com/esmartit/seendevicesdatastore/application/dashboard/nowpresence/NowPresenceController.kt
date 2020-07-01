package com.esmartit.seendevicesdatastore.application.dashboard.nowpresence

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant

@Deprecated(replaceWith = ReplaceWith("DetectedController"), message = "")
@RestController
@RequestMapping("/sensor-activity")
class NowPresenceController(private val minuteService: NowPresenceService) {

    @GetMapping(path = ["/minute-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getRecentActivity(): Flux<NowPresence> {

        val thirtyMinutesAgo = Instant.now().minus(Duration.ofMinutes(30))
        return minuteService.getPresenceAfter(thirtyMinutesAgo)
    }

    @GetMapping(path = ["/minute-device-total-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getRecentTotalActivity(): Flux<MinuteRecentCount> {
        val tenMinutesAgo = Instant.now().minus(Duration.ofMinutes(10))
        return minuteService.getPresenceAfter(tenMinutesAgo)
            .map { it.run { inCount + limitCount + outCount }.run {
                MinuteRecentCount(this, it.time)
            } }
    }
}

data class MinuteRecentCount(val count: Long, val time: Instant)
