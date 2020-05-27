package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.MinutePresenceCountTailable
import com.esmartit.seendevicesdatastore.service.MinutePresenceService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/sensor-activity")
class MinutePresenceCountController(private val minuteService: MinutePresenceService) {

    @GetMapping(path = ["/minute-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getRecentActivity(): Flux<MinutePresenceCountTailable> {

        val thirtyMinutesAgo = Instant.now().minus(Duration.ofMinutes(30))
        return minuteService.getPresenceAfter(thirtyMinutesAgo)
    }

    @GetMapping(path = ["/minute-device-total-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getRecentTotalActivity(): Flux<MinuteRecentCount> {
        val tenMinutesAgo = Instant.now().minus(Duration.ofMinutes(10))
        return minuteService.getPresenceAfter(tenMinutesAgo)
            .map { it.run { inCount + limitCount + outCount }.run { MinuteRecentCount(this, it.time) } }
    }
}

data class MinuteRecentCount(val count: Long, val time: Instant)
