package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.event_store.StoredEvent
import com.esmartit.seendevicesdatastore.application.event_store.StoredEventRepository
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class ScanApiConsumer(
    private val scanApiStoreService: ScanApiStoreService,
    private val repository: StoredEventRepository,
    private val objectMapper: ObjectMapper
) {

    @StreamListener(Sink.INPUT)
    fun handle(event: SensorActivityEvent) {
        val scanApiActivity = scanApiStoreService.createScanApiActivity(event)
        repository.save(StoredEvent(payload = objectMapper.writeValueAsString(scanApiActivity)))
    }
}

