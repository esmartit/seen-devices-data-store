package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document
data class HourlyDeviceCount(
    val id: String? = null,
    val time: Instant = Instant.now(),
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)

@Document
data class HourlyDeviceCountTailable(
    val id: String? = null,
    val time: Instant,
    val inCount: Long,
    val limitCount: Long,
    val outCount: Long
)

interface HourlyDeviceCountRepository : MongoRepository<HourlyDeviceCount, String> {
    fun findByTime(time: Instant): HourlyDeviceCount?
}

interface HourlyDeviceCountReactiveRepository : ReactiveMongoRepository<HourlyDeviceCountTailable, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<HourlyDeviceCountTailable>
}