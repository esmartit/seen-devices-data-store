package com.esmartit.seendevicesdatastore.consumer

import java.time.Instant

data class DeviceSeenEvent(val apMac: String,
                           val groupName:String,
                           val hotSpot:String,
                           val sensorName:String,
                           val spotId:String,
                           val device: DeviceSeen,
                           val apFloors: List<String?>)

data class DeviceSeen(val clientMac: String,
                      val ipv4: String?,
                      val ipv6: String?,
                      val location: DeviceLocation,
                      val manufacturer: String?,
                      val os: String?,
                      val rssi: Int,
                      val seenEpoch: Int,
                      val seenTime: Instant,
                      val ssid: String?)

data class DeviceLocation(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)