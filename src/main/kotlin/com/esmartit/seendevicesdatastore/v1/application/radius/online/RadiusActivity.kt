package com.esmartit.seendevicesdatastore.v1.application.radius.online

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{'info.callingStationId':1, 'eventTimeStamp':1}", name = "radius_activity_callingStationId_eventTimeStamp_idx")
data class RadiusActivity(val id: String? = null, val info: RadiusActivityInfo)

data class RadiusActivityInfo(
    @Indexed(name = "username_idx")
    val username: String,
    val acctSessionId: String,
    val statusType: String,
    val acctUniqueSessionId: String,
    val calledStationId: String,
    @Indexed(name = "callingStationId_idx")
    val callingStationId: String,
    val connectInfo: String,
    @Indexed(name = "eventTimeStamp_idx")
    val eventTimeStamp: Instant,
    val serviceType: String
)
