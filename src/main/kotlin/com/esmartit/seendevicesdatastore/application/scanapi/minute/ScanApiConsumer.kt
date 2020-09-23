package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.event_store.StoredEvent
import com.esmartit.seendevicesdatastore.application.event_store.StoredEventReactiveRepository
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import reactor.core.publisher.Mono

@EnableBinding(Sink::class)
class ScanApiConsumer(
    private val scanApiStoreService: ScanApiStoreService,
    private val repository: StoredEventReactiveRepository,
    private val objectMapper: ObjectMapper
) {

    @StreamListener(Sink.INPUT)
    fun handle(event: SensorActivityEvent) {
        scanApiStoreService.createScanApiActivity(event)
            .flatMap { Mono.fromCallable { objectMapper.writeValueAsString(it) } }
            .flatMap { repository.save(StoredEvent(payload = it)) }
            .subscribe()
    }
}

