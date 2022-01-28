package com.esmartit.seendevicesdatastore.application.scanapi.hourly

import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivityH
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ScanApiActivityHourlyRepository :
        MongoRepository<ScanApiActivityH, String> {
    fun findByClientMacAndDateAtZoneAndSpotIdAndSensorIdAndStatus(clientMac: String, dateAtZone: Instant, spotId: String?, sensorId: String?, status: Position): ScanApiActivityH?

}
