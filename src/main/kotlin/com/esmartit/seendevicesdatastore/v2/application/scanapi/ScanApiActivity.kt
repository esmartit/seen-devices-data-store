package com.esmartit.seendevicesdatastore.v2.application.scanapi

import com.esmartit.seendevicesdatastore.v1.application.radius.registered.RegisteredInfo
import com.esmartit.seendevicesdatastore.v1.application.sensorsettings.SensorSetting
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'device.clientMac':1, 'seenTime':1}", name = "scan_api_activity_clientMac_seenTime_idx")
data class ScanApiActivity(
    val id: String? = null,
    val apMac: String,
    val seenEpoch: Int,
    val seenTime: Instant,
    val rssi: Int,
    val ssid: String?,
    val device: Device,
    val apFloors: List<String?>,
    val sensorSetting: SensorSetting? = null,
    val location: Location,
    val userInfo: RegisteredInfo? = null
)

data class Device(
    val clientMac: String,
    val ipv4: String?,
    val ipv6: String?,
    val manufacturer: String?,
    val os: String?
)

data class Location(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)
