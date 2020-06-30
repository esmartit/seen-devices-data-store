package com.esmartit.seendevicesdatastore.application.radius.online

import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class RadiusActivity(val id: String? = null, val info: RadiusActivityInfo)

data class RadiusActivityInfo(
    val username: String,
    val acctSessionId: String,
    val statusType: String,
    val acctUniqueSessionId: String,
    val calledStationId: String,
    val callingStationId: String,
    val connectInfo: String,
    val eventTimeStamp: Instant,
    val serviceType: String
)
