package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface HourlyScanApiReactiveRepository :
    ReactiveMongoRepository<HourlyScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(time: Instant): Flux<HourlyScanApiActivity>
    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<HourlyScanApiActivity>
    fun findBySeenTimeLessThanEqual(end: Instant): Flux<HourlyScanApiActivity>
}