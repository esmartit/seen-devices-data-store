package com.esmartit.seendevicesdatastore.application.event_store

import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiStoreService
import com.esmartit.seendevicesdatastore.domain.SensorActivity
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventStoreScheduler(
    private val repository: StoredEventReactiveRepository,
    private val service: ScanApiStoreService,
    private val objectMapper: ObjectMapper
) {

    @Scheduled(initialDelay = 120000, fixedDelay = 15000)
    @SchedulerLock(name = "processEvents", lockAtMostFor = "10m", lockAtLeastFor = "10s")
    fun processEvent() {
        repository.findByProcessed(false)
            .take(10000)
            .doOnNext {
                val payload = objectMapper.readValue<SensorActivity>(it.payload)
                service.save(payload)
            }.flatMap {
                repository.save(it.copy(processed = true))
            }.subscribe()
    }

    @Scheduled(initialDelay = 240000, fixedDelay = 60000)
    @SchedulerLock(name = "deleteProcessedEventsTask", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    fun deleteProcessed() {
        repository.deleteByProcessed(true).subscribe {
            println("$it deleted events")
        }
    }
}