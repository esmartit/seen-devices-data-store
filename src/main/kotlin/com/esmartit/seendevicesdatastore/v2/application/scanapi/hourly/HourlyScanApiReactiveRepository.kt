package com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface HourlyScanApiReactiveRepository :
    ReactiveMongoRepository<HourlyScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(time: Instant): Flux<HourlyScanApiActivity>
    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<HourlyScanApiActivity>
    fun findBySeenTimeLessThanEqual(end: Instant): Flux<HourlyScanApiActivity>
}