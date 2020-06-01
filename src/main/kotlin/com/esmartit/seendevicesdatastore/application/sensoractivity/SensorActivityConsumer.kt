package com.esmartit.seendevicesdatastore.application.sensoractivity

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceSeenEvent
import com.esmartit.seendevicesdatastore.application.incomingevents.EventToSensorActivity
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SensorActivityConsumer(
    private val eventToSensorActivity: EventToSensorActivity,
    private val repository: SensorActivityRepository
) {

    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: DeviceSeenEvent) {

        val incomingSensorActivity = eventToSensorActivity.convertToSensorActivity(seenDevice)
        val existingSensorActivity = repository.findByDeviceMacAddressAndSeenTime(
            incomingSensorActivity.device.macAddress,
            incomingSensorActivity.seenTime
        )
        val existingRSSI = existingSensorActivity?.rssi ?: -1000

        if (incomingSensorActivity.rssi > existingRSSI) {
            repository.save(incomingSensorActivity.copy(id = existingSensorActivity?.id))
        }
    }
}