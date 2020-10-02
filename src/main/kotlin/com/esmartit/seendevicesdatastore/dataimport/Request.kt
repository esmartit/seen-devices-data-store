package com.esmartit.seendevicesdatastore.dataimport

import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceLocation
import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceSeen
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import java.time.Instant

data class MerakiPayload(
    val data: Data,
    val secret: String,
    val type: String,
    val version: String
)

data class Data(
    val apFloors: List<String?>,
    val apMac: String,
    val apTags: List<String>,
    val observations: List<Observation>
) {
    fun mapToDeviceSeenEvents(): List<SensorActivityEvent> {
        val apMac = apMac
        val apFloors = apFloors

        return observations.map { it.toDeviceSeen() }
            .map {
                SensorActivityEvent(
                    apMac = apMac,
                    device = it,
                    apFloors = apFloors
                )
            }
    }
}

data class Observation(
    val clientMac: String,
    val ipv4: String?,
    val ipv6: String?,
    val location: Location,
    val manufacturer: String?,
    val os: String?,
    val rssi: Int,
    val seenEpoch: Int,
    val seenTime: String,
    val ssid: String?
)

data class Location(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)

fun Data.mapToDeviceSeenEvents(): List<SensorActivityEvent> {

    val apMac = apMac
    val apFloors = apFloors

    return observations.map { it.toDeviceSeen() }
        .map {
            SensorActivityEvent(
                apMac = apMac,
                device = it,
                apFloors = apFloors
            )
        }
}

fun Observation.toDeviceSeen() =
    DeviceSeen(
        clientMac,
        ipv4,
        ipv6,
        location.toDeviceLocation(),
        manufacturer,
        os,
        rssi - 95,
        seenEpoch,
        Instant.parse(seenTime),
        ssid
    )

fun Location.toDeviceLocation() =
    DeviceLocation(lat, lng, unc, x, y)
