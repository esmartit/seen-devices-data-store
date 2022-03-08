package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class UniqueDevice(
        val id: String,
        @Indexed(name = "seenTime_idx")
        val seenTime: Instant
)

