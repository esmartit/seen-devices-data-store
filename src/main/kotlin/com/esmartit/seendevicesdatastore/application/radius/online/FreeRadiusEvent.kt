package com.esmartit.seendevicesdatastore.application.radius.online

data class FreeRadiusEvent(
    val acctSessionId: String,
    val statusType: String,
    val acctUniqueSessionId: String,
    val calledStationId: String,
    val callingStationId: String,
    val connectInfo: String,
    val eventTimeStamp: String,
    val serviceType: String,
    val username: String
)