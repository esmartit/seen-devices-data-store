package com.esmartit.seendevicesdatastore.domain

import com.esmartit.seendevicesdatastore.v1.application.radius.registered.Gender
import com.esmartit.seendevicesdatastore.v1.repository.Position
import java.time.Instant

data class FlatDevice(
    val clientMac: String,
    val seenTime: Instant,
    val age: Int = 1900,
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
