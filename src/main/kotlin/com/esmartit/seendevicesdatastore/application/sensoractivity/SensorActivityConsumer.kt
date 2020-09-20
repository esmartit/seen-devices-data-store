package com.esmartit.seendevicesdatastore.application.sensoractivity

import com.esmartit.seendevicesdatastore.domain.SensorActivity
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SensorActivityConsumer(
    private val repository: SensorActivityRepository
) {

    @StreamListener(Sink.INPUT)
    fun handle(sensorActivity: SensorActivity) {
        val clientMac = sensorActivity.device.clientMac
        val seenTime = sensorActivity.device.seenTime
        val existingActivity = repository.findByDeviceClientMacAndDeviceSeenTime(clientMac, seenTime)
        if (existingActivity.isEmpty()) {
            repository.save(sensorActivity)
        }
    }
}