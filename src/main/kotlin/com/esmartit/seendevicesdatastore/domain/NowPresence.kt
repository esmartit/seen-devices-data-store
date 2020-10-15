package com.esmartit.seendevicesdatastore.domain

data class NowPresence(
    val id: String? = null,
    val time: String = "",
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)

