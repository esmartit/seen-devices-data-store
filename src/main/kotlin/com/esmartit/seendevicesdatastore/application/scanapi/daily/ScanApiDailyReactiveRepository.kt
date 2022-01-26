package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityD
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ScanApiDailyReactiveRepository : ReactiveMongoRepository<ScanApiActivityD, String> {
    fun findByDateAtZoneGreaterThanEqual(dateAtZone: String): Flux<ScanApiActivityD>
    fun findByDateAtZoneBetween(startDateFilter: String, end: String): Flux<ScanApiActivityD>
    fun findByDateAtZoneLessThanEqual(end: String): Flux<ScanApiActivityD>
}

@Repository
interface ScanApiDailyRepository : MongoRepository<ScanApiActivityD, String> {
    fun findByClientMacAndDateAtZone(clientMac: String, dateAtZone: String): ScanApiActivityD?
}