package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
        unique = true,
        def = "{'clientMac':1, 'seenTime':1}", name = "scan_api_activity_clientMac_seenTimeYear_idx"
)

data class UniqueDeviceYearly (
        val id: String,
        @Indexed(name = "clientMac_idx")
        val clientMac: String,
        @Indexed(name = "seenTime_idx")
        val seenTime: Instant,
        val yearSeenTime: Int
)