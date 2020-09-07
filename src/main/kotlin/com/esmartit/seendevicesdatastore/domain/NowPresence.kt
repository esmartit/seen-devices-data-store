package com.esmartit.seendevicesdatastore.domain

import java.time.Instant

data class NowPresence(
    val id: String? = null,
    val time: Instant = Instant.now(),
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)

