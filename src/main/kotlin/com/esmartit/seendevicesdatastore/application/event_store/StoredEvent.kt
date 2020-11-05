package com.esmartit.seendevicesdatastore.application.event_store

import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Document
data class StoredEvent(
    val id: String,
    @Indexed
    val processed: Boolean = false,
    val payload: SensorActivityEvent
)


@Repository
interface StoredEventReactiveRepository : ReactiveMongoRepository<StoredEvent, String>

@Repository
interface StoredEventRepository : MongoRepository<StoredEvent, String>

