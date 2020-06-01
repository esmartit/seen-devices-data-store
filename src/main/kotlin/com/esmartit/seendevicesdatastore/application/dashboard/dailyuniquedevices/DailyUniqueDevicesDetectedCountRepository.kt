package com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document
data class DailyUniqueDevicesDetectedCount(val count: Long, val time: Instant)

interface DailyUniqueDevicesDetectedCountRepository : MongoRepository<DailyUniqueDevicesDetectedCount, String>

interface DailyUniqueDevicesDetectedCountReactiveRepository :
    ReactiveMongoRepository<DailyUniqueDevicesDetectedCount, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<DailyUniqueDevicesDetectedCount>
}