package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface ScanApiReactiveRepository : ReactiveMongoRepository<ScanApiActivity, String> {
    fun findBySeenTimeGreaterThanEqual(seenTime: Instant): Flux<ScanApiActivity>
    fun findBySeenTimeBetween(startDateTimeFilter: Instant, end: Instant): Flux<ScanApiActivity>
    fun findBySeenTimeLessThanEqual(end: Instant): Flux<ScanApiActivity>
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): Mono<ScanApiActivity>
    fun findByProcessed(processed: Boolean): Flux<ScanApiActivity>
}

@Repository
interface ScanApiRepository : MongoRepository<ScanApiActivity, String> {
    fun findByClientMacAndSeenTime(clientMac: String, seenTime: Instant): ScanApiActivity?
}