package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UniqueDevice(
    val id: String
)

