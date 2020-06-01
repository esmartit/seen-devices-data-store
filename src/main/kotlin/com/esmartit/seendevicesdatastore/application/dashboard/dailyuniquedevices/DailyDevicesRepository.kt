package com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document(collection = "dailyUniqueDevicesDetectedCount")
data class DailyDevices(val count: Long, val time: Instant)

interface DailyDevicesRepository : MongoRepository<DailyDevices, String>

interface DailyDevicesReactiveRepository : ReactiveMongoRepository<DailyDevices, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<DailyDevices>
}