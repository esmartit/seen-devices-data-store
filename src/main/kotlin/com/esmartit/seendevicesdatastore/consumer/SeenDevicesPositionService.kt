package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceWithPresenceEvent
import com.esmartit.seendevicesdatastore.application.incomingevents.EventToSensorActivity
import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.repository.DevicePositionRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.stereotype.Service

@Service
class SeenDevicesPositionService(
    private val eventToSensorActivity: EventToSensorActivity,
    private val repository: DevicePositionRepository,
    private val radiusActivity: RadiusActivityRepository
) {

    fun handle(event: DeviceWithPresenceEvent) {

        val incomingSensorActivity = eventToSensorActivity.convertToSensorActivity(event.deviceDetectedEvent)
        val existingSensorActivity = repository.findByMacAddressAndSeenTime(
            incomingSensorActivity.device.macAddress,
            incomingSensorActivity.seenTime
        )

        val existingRSSI = existingSensorActivity?.position ?: Position.NO_POSITION
        val newPosition = if (event.position.value > existingRSSI.value) event.position else existingRSSI
        val newCount = existingSensorActivity?.let { it.countInAnHour + 1 } ?: 1
        val userInfo = radiusActivity.findByInfoCallingStationId(incomingSensorActivity.device.macAddress)

        repository.save(
            DeviceWithPosition(
                id = existingSensorActivity?.id,
                macAddress = incomingSensorActivity.device.macAddress,
                position = newPosition,
                seenTime = incomingSensorActivity.seenTime,
                countInAnHour = newCount,
                userInfo = userInfo,
                lastUpdate = incomingSensorActivity.lastUpdate,
                countryLocation = event.deviceDetectedEvent.countryLocation
            )
        )
    }
}
