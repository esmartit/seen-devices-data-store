package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document
data class MinutePresenceCountTailable(
    val id: String? = null,
    val time: Instant = Instant.now(),
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)

interface MinutePresenceCountRepository : ReactiveMongoRepository<MinutePresenceCountTailable, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<MinutePresenceCountTailable>

    fun findByTimeGreaterThanEqual(time: Instant): Flux<MinutePresenceCountTailable>
}