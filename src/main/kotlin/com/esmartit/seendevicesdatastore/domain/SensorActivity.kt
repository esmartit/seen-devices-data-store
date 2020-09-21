package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(
//    unique = true,
    def = "{'device.clientMac':1, 'device.seenTime':1}",
    name = "sensor_activity_macAddress_seenTime_idx"
)
data class SensorActivity(
    val id: String? = null,
    val apMac: String,
    val device: DeviceSeen,
    val apFloors: List<String?>
)

data class DeviceSeen(
    val clientMac: String,
    val ipv4: String?,
    val ipv6: String?,
    val location: DeviceLocation,
    val manufacturer: String?,
    val os: String?,
    val rssi: Int,
    val seenEpoch: Int,
    val seenTime: Instant,
    val ssid: String?
)

data class DeviceLocation(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)
