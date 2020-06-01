package com.esmartit.seendevicesdatastore.application.incomingevents

import java.time.Instant

data class PresenceEvent(
    val time: Instant,
    val inCount: Long,
    val limitCount: Long,
    val outCount: Long
)