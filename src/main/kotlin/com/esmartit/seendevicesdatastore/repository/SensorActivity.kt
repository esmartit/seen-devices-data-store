package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class SensorActivity(
    val id: String? = null,
    val accessPoint: AccessPoint,
    val device: Device,
    val seenTime: Instant,
    val rssi: Int,
    val location: Location
)

data class AccessPoint(
    val macAddress: String? = null,
    val groupName: String? = null,
    val hotSpot: String? = null,
    val sensorName: String? = null,
    val spotId: String? = null,
    val floors: List<String?> = emptyList()
)

data class Device(
    val macAddress: String,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val os: String? = null,
    val manufacturer: String? = null
)

data class Location(val position: List<Double?> = emptyList(), val unc: Double? = null)