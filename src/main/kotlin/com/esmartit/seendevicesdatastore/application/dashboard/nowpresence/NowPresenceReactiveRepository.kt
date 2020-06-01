package com.esmartit.seendevicesdatastore.application.dashboard.nowpresence

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import reactor.core.publisher.Flux
import java.time.Instant

@Document(collection = "minutePresenceCountTailable")
data class NowPresence(
    val id: String? = null,
    val time: Instant = Instant.now(),
    val inCount: Long = 0,
    val limitCount: Long = 0,
    val outCount: Long = 0
)

interface NowPresenceReactiveRepository : ReactiveMongoRepository<NowPresence, String> {
    @Tailable
    fun findWithTailableCursorBy(): Flux<NowPresence>

    fun findByTimeGreaterThanEqual(time: Instant): Flux<NowPresence>
}