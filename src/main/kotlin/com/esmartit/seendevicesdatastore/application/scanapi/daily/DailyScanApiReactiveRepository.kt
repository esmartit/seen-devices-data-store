package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface DailyScanApiReactiveRepository :
    ReactiveMongoRepository<DailyScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(seenTime: Instant): Flux<DailyScanApiActivity>
    fun findBySeenTimeLessThanEqual(time: Instant): Flux<DailyScanApiActivity>
    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<DailyScanApiActivity>
}