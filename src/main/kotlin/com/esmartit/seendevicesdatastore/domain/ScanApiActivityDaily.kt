package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
        unique = false,
        def = "{'clientMac':1, 'dateAtZone':1}", name = "scan_api_activity_clientMac_dateAtZone_idx"
)
data class ScanApiActivityDaily(
        val id: String,
        @Indexed(name = "dateAtZone_idx")
        val dateAtZone: Instant,
        @Indexed(name = "clientMac_idx")
        val clientMac: String,
        val status: String,
        val countryId: String? = null,
        val stateId: String? = null,
        val cityId: String? = null,
        val zipCode: String? = "",
        val spotId: String? = null,
        val sensorId: String? = null,
        val zone: String? = null,
        val ssid: String? = null,
        val brand: String? = null,
        val username: String? = null,
        val gender: Gender? = null,
        val age: Int = 1900,
        val memberShip: Boolean? = null,
        val userZipCode: String? = null,
        val totalTime: Long? = 60000,
        val minTime: Instant,
        val maxTime: Instant
)
