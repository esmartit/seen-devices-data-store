package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivityD
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Repository
interface ScanApiActivityDailyRepository :
        MongoRepository<ScanApiActivityD, String> {
    fun findByClientMacAndDateAtZoneAndSpotIdAndSensorIdAndStatus(clientMac: String, dateAtZone: LocalDateTime, spotId: String?, sensorId: String?, status: Position): ScanApiActivityD?

}
