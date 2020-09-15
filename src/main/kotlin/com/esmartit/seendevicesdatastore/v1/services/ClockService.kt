package com.esmartit.seendevicesdatastore.v1.services

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class ClockService(private val clock: Clock) {

    fun thirtyMinutesAgo(zoneId: ZoneId): ZonedDateTime {
        return clock.instant().atZone(zoneId).minusMinutes(30L)
    }

    fun startOfDay(zoneId: ZoneId): ZonedDateTime {
        return clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId)
    }

    fun minutesAgo(zoneId: ZoneId, minutes: Long): ZonedDateTime {
        return clock.instant().atZone(zoneId).minusMinutes(minutes)
    }

    fun now(): Instant {
        return clock.instant()
    }
}