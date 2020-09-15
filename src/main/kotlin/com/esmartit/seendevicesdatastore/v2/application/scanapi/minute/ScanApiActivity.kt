package com.esmartit.seendevicesdatastore.v2.application.scanapi.minute

import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.v1.application.radius.registered.Gender
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'clientMac':1, 'seenTime':1}", name = "scan_api_activity_clientMac_seenTime_idx")
data class ScanApiActivity(
    val id: String? = null,
    @Indexed(name = "clientMac_idx")
    val clientMac: String,
    @Indexed(name = "seenTime_idx")
    val seenTime: Instant,
    val age: Int = 1900,
    val rssi: Int = -10000,
    val countryId: String? = null,
    val stateId: String? = null,
    val cityId: String? = null,
    val spotId: String? = null,
    val sensorId: String? = null,
    val brand: String? = null,
    val status: Position = Position.NO_POSITION,
    val gender: Gender? = null,
    val zipCode: String? = "",
    val memberShip: Boolean? = null,
    val registeredDate: Instant? = null,
    val countInAnHour: Int = 0,
    val isConnected: Boolean = false,
    val username: String? = null
) {
    fun isInRange(): Boolean {
        return status != Position.NO_POSITION
    }
}
