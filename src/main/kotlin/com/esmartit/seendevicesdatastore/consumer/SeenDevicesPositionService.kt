package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceWithPresenceEvent
import com.esmartit.seendevicesdatastore.application.incomingevents.EventToSensorActivity
import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivity
import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityInfo
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.repository.DevicePositionRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SeenDevicesPositionService(
    private val eventToSensorActivity: EventToSensorActivity,
    private val repository: DevicePositionRepository,
    private val registeredUserRepository: RegisteredUserRepository
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
        val registered =
            registeredUserRepository.findByInfoClientMac(incomingSensorActivity.device.macAddress).lastOrNull()
        repository.save(
            DeviceWithPosition(
                id = existingSensorActivity?.id,
                macAddress = incomingSensorActivity.device.macAddress,
                position = newPosition,
                seenTime = incomingSensorActivity.seenTime,
                countInAnHour = newCount,
                userInfo = registered?.info,
                lastUpdate = incomingSensorActivity.lastUpdate,
                activity = incomingSensorActivity
            )
        )
    }
}
