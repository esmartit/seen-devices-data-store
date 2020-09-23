package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
    unique = true, def = "{'clientMac':1, 'seenTime':1}", name = "sensor_activity_clientMac_seenTime_idx"
)
data class SensorActivity(
    val id: String,
    val clientMac: String,
    val seenTime: Instant,
    val apMac: String,
    val rssi: Int,
    val ssid: String?,
    val manufacturer: String?,
    val os: String?,
    val ipv4: String?,
    val ipv6: String?,
    val location: DeviceLocation,
    val apFloors: List<String?>,
    val processed: Boolean = false
)

data class DeviceLocation(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)
