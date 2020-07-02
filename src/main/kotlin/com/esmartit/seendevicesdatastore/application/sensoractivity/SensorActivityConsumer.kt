package com.esmartit.seendevicesdatastore.application.sensoractivity

import com.esmartit.seendevicesdatastore.application.incomingevents.EventToSensorActivity
import com.esmartit.seendevicesdatastore.application.incomingevents.SensorActivityEvent
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SensorActivityConsumer(
    private val eventToSensorActivity: EventToSensorActivity,
    private val repository: SensorActivityRepository
) {

    private val logger = LoggerFactory.getLogger(SensorActivityConsumer::class.java)

    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: SensorActivityEvent) {

        val incomingSensorActivity = eventToSensorActivity.convertToSensorActivity(seenDevice)
        val existingSensorActivity = repository.findByDeviceMacAddressAndSeenTime(
            incomingSensorActivity.device.macAddress,
            incomingSensorActivity.seenTime
        )
        val existingRSSI = existingSensorActivity?.rssi ?: -1000

        if (incomingSensorActivity.rssi > existingRSSI) {
            repository.save(incomingSensorActivity.copy(id = existingSensorActivity?.id))
        }

        logger.info("Event Consumed: $seenDevice")
    }
}