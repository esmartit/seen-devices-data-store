package com.esmartit.seendevicesdatastore.v1.application.sensoractivity

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SensorActivityConsumer(
    private val repository: SensorActivityRepository
) {

    @StreamListener(Sink.INPUT)
    fun handle(sensorActivity: SensorActivity) {
        repository.save(sensorActivity)
    }
}