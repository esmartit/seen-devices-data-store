package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
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
    private val sensorSettingRepository: SensorSettingRepository,
    private val brandsRepository: BrandsRepository
) {

    @StreamListener(Sink.INPUT)
    @SendTo("scanApiFlow.input")
    fun handle(event: SensorActivityEvent): ScanApiActivity {
        return scanApiRepository.save(event.toScanApiActivity())
    }

    private fun SensorActivityEvent.toScanApiActivity(): ScanApiActivity {
        val sensorSetting = sensorSettingRepository.findByApMac(apMac)
        val brand = brandsRepository.findByName(device.manufacturer ?: "")
        return ScanApiActivity(
            id = "${device.clientMac};${device.seenTime.epochSecond}",
            clientMac = device.clientMac,
            seenTime = device.seenTime,
            brand = brand.name,
            manufacturer = device.manufacturer,
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
}


