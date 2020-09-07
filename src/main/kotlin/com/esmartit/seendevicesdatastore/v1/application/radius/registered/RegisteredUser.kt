package com.esmartit.seendevicesdatastore.v1.application.radius.registered

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document
data class RegisteredUser(val id: String? = null, val info: RegisteredInfo)

data class RegisteredInfo(
    @Indexed(name = "username_idx")
    val username: String,
    @Indexed(name = "clientMac_idx")
    val clientMac: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val zipCode: String,
    val memberShip: Boolean,
    val spotId: String,
    val hotspotName: String,
    val seenTime: Instant
)

enum class Gender {
    MALE, FEMALE
}