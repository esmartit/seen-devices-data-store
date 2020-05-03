package com.esmartit.seendevicesdatastore.http

import java.time.Instant

data class DeviceSeenEventReStream(val apMac: String,
                           val groupName:String,
                           val hotSpot:String,
                           val sensorName:String,
                           val spotId:String,
                           val device: DeviceSeenReStream,
                           val apFloors: List<String?>)

data class DeviceSeenReStream(val clientMac: String,
                      val ipv4: String?,
                      val ipv6: String?,
                      val location: DeviceLocationReStream,
                      val manufacturer: String?,
                      val os: String?,
                      val rssi: Int,
                      val seenEpoch: Int,
                      val seenTime: String,
                      val ssid: String?)

data class DeviceLocationReStream(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?> = emptyList(),
    val y: List<String?> = emptyList()
)