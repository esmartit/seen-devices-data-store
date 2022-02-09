package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityH
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface ScanApiHourlyReactiveRepository : ReactiveMongoRepository<ScanApiActivityH, String> {
    fun findByDateAtZoneGreaterThanEqual(dateAtZone: Instant): Flux<ScanApiActivityH>
    fun findByDateAtZoneBetween(startDateFilter: Instant, end: Instant): Flux<ScanApiActivityH>
    fun findByDateAtZoneLessThanEqual(end: Instant): Flux<ScanApiActivityH>
}

@Repository
interface ScanApiHpurlyRepository : MongoRepository<ScanApiActivityH, String> {
    fun findByClientMacAndDateAtZone(clientMac: String, dateAtZone: Instant): ScanApiActivityH?
}