package com.esmartit.seendevicesdatastore.application.incomingevents

data class RegisteredEvent(
    val username: String,
    val clientMac: String,
    val dateOfBirth: String,
    val gender: String,
    val zipCode: String,
    val memberShip: String,
    val spotId: String,
    val hotspotName: String,
    val seenTimeEpoch: Long
)