package com.esmartit.seendevicesdatastore.application.event_store

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Document
data class StoredEvent(val id: String? = null, val processed: Boolean = false, val payload: String)


@Repository
interface StoredEventReactiveRepository : ReactiveMongoRepository<StoredEvent, String> {
    fun findByProcessed(processed: Boolean): Flux<StoredEvent>
    fun deleteByProcessed(processed: Boolean): Mono<Long>
}

@Repository
interface StoredEventRepository : MongoRepository<StoredEvent, String>

