package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityH
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ScanApiHourlyReactiveRepository : ReactiveMongoRepository<ScanApiActivityH, String> {
    fun findByDateAtZoneGreaterThanEqual(dateAtZone: String): Flux<ScanApiActivityH>
    fun findByDateAtZoneBetween(startDateFilter: String, end: String): Flux<ScanApiActivityH>
    fun findByDateAtZoneLessThanEqual(end: String): Flux<ScanApiActivityH>
}

@Repository
interface ScanApiHpurlyRepository : MongoRepository<ScanApiActivityH, String> {
    fun findByClientMacAndDateAtZone(clientMac: String, dateAtZone: String): ScanApiActivityH?
}