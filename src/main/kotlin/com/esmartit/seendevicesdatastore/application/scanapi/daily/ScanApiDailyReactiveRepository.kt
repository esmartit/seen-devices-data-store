package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityD
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface ScanApiDailyReactiveRepository : ReactiveMongoRepository<ScanApiActivityD, String> {
    fun findByDateAtZoneGreaterThanEqual(dateAtZone: Instant): Flux<ScanApiActivityD>
    fun findByDateAtZoneBetween(startDateFilter: Instant, end: Instant): Flux<ScanApiActivityD>
    fun findByDateAtZoneLessThanEqual(end: Instant): Flux<ScanApiActivityD>
}

@Repository
interface ScanApiDailyRepository : MongoRepository<ScanApiActivityD, String> {
    fun findByClientMacAndDateAtZone(clientMac: String, dateAtZone: Instant): ScanApiActivityD?
}