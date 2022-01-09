package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivityDaily
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZonedDateTime

@Repository
interface ScanApiActivityDailyRepository :
        MongoRepository<ScanApiActivityDaily, String> {
    fun findByClientMacDateAtZoneSpotSensorAndStatus(clientMac: String, dateAtZone: ZonedDateTime, spotId: String?, sensorId: String?, status: Position): ScanApiActivityDaily?

}
