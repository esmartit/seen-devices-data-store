package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSetting
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.messaging.handler.annotation.SendTo

@EnableBinding(Sink::class)
class ScanApiConsumer(
    private val scanApiRepository: ScanApiRepository,
    private val sensorSettingRepository: SensorSettingRepository
) {

    @StreamListener(Sink.INPUT)
    @SendTo("scanApiFlow.input")
    fun handle(event: SensorActivityEvent): ScanApiActivity {
        val sensorSetting = sensorSettingRepository.findByApMac(event.apMac)
        return scanApiRepository.save(event.toScanApiActivity(sensorSetting))
    }
}

private fun SensorActivityEvent.toScanApiActivity(sensorSetting: SensorSetting?): ScanApiActivity {
    return ScanApiActivity(
        id = "${device.clientMac};${device.seenTime.epochSecond}",
        clientMac = device.clientMac,
        seenTime = device.seenTime,
        brand = device.manufacturer,
        isConnected = !device.ssid.isNullOrBlank(),
        rssi = device.rssi,
        status = sensorSetting?.presence(device.rssi) ?: Position.NO_POSITION,
        spotId = sensorSetting?.tags?.get("spot_id"),
        sensorId = sensorSetting?.tags?.get("sensorname"),
        countryId = sensorSetting?.tags?.get("country"),
        stateId = sensorSetting?.tags?.get("state"),
        cityId = sensorSetting?.tags?.get("city"),
        zipCode = sensorSetting?.tags?.get("zipcode"),
        groupName = sensorSetting?.tags?.get("groupname"),
        hotspot = sensorSetting?.tags?.get("hotspot"),
        ssid = device.ssid
    )
}

