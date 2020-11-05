package com.esmartit.seendevicesdatastore.application.event_store

import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class EventStoreConsumer(private val repository: StoredEventRepository) {

    @StreamListener(Sink.INPUT)
    fun handle(event: SensorActivityEvent) {
        val eventId = with(event) { "${device.clientMac};${device.seenTime.epochSecond}" }
        repository.save(StoredEvent(id = eventId, payload = event))
    }
}
