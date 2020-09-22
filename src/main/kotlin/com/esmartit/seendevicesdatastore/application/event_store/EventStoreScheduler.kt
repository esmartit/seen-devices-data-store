package com.esmartit.seendevicesdatastore.application.event_store

import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiStoreService
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventStoreScheduler(
    private val repository: StoredEventReactiveRepository,
    private val service: ScanApiStoreService,
    private val objectMapper: ObjectMapper
) {

    @Value("\${eventStore.processEvents.batchSize}")
    private var batchSize: Long = 1000

    @Value("\${eventStore.processEvents.enabled}")
    private var processEventsEnabled: Boolean = true

    @Value("\${eventStore.deleteProcessed.enabled}")
    private var deleteProcessedEnabled: Boolean = true

    @Scheduled(
        initialDelayString = "\${eventStore.processEvents.initialDelay}",
        fixedDelayString = "\${eventStore.processEvents.fixedDelay}"
    )
    @SchedulerLock(
        name = "processEvents",
        lockAtMostFor = "\${eventStore.processEvents.lockAtMostFor}",
        lockAtLeastFor = "\${eventStore.processEvents.lockAtLeastFor}"
    )
    fun processEvent() {
        if (processEventsEnabled) {
            println("processing events...")
            repository.findByProcessed(false)
                .limitRequest(batchSize)
                .flatMap { event ->
                    objectMapper.readValue<ScanApiActivity>(event.payload)
                        .let { service.save(it) }
                        .flatMap { repository.save(event.copy(processed = true)) }
                }
                .count()
                .block()
                .also {
                    println("$it processed events")
                }
        }
    }

    @Scheduled(
        initialDelayString = "\${eventStore.deleteProcessed.initialDelay}",
        fixedDelayString = "\${eventStore.deleteProcessed.fixedDelay}"
    )
    @SchedulerLock(
        name = "deleteProcessedEventsTask",
        lockAtMostFor = "\${eventStore.deleteProcessed.lockAtMostFor}",
        lockAtLeastFor = "\${eventStore.deleteProcessed.lockAtLeastFor}"
    )
    fun deleteProcessed() {
        if (deleteProcessedEnabled) {
            repository.deleteByProcessed(true).subscribe {
                println("$it deleted events")
            }
        }
    }
}