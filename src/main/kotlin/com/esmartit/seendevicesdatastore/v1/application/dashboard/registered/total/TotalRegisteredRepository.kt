package com.esmartit.seendevicesdatastore.v1.application.dashboard.registered.total

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import java.time.Instant

@Document(collection = "totalRegisteredCount")
data class TotalRegistered(val count: Long, val time: Instant)

interface TotalRegisteredRepository : MongoRepository<TotalRegistered, String>

interface TotalRegisteredReactiveRepository : ReactiveMongoRepository<TotalRegistered, String>