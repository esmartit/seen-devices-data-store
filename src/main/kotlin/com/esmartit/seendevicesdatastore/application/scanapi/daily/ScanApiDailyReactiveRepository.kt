package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityDaily
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ScanApiDailyReactiveRepository : ReactiveMongoRepository<ScanApiActivityDaily, String> {
    fun findByDateAtZoneGreaterThanEqual(dateAtZone: String): Flux<ScanApiActivityDaily>
    fun findByDateAtZoneBetween(startDateFilter: String, end: String): Flux<ScanApiActivityDaily>
    fun findByDateAtZoneLessThanEqual(end: String): Flux<ScanApiActivityDaily>
}

@Repository
interface ScanApiDailyRepository : MongoRepository<ScanApiActivityDaily, String> {
    fun findByClientMacAndDateAtZone(clientMac: String, dateAtZone: String): ScanApiActivityDaily?
}