package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'device.macAddress':1, 'seenTime':1}", name = "sensor_activity_macAddress_seenTime_idx")
data class SensorActivity(
    val id: String? = null,
    val accessPoint: AccessPoint,
    val device: Device,
    val seenTime: Instant,
    val rssi: Int,
    val location: Location,
    val lastUpdate: Instant = seenTime,
    val ssid: String? = null,
    val processed: Boolean = false
)

data class AccessPoint(
    val macAddress: String? = null,
    val groupName: String? = null,
    val hotSpot: String? = null,
    val sensorName: String? = null,
    val spotId: String? = null,
    val floors: List<String?> = emptyList(),
    val countryLocation: CountryLocation? = null
)

data class Device(
    val macAddress: String,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val os: String? = null,
    val manufacturer: String? = null
)

data class Location(val position: List<Double?> = emptyList(), val unc: Double? = null)

data class CountryLocation(val countryId: String, val stateId: String, val cityId: String, val zipCode: String = "")