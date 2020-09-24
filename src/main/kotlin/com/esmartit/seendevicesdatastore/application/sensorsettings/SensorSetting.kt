package com.esmartit.seendevicesdatastore.application.sensorsettings

import com.esmartit.seendevicesdatastore.domain.Position
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class SensorSetting(
    val id: String? = null,
    val inEdge: Int,
    val limitEdge: Int,
    val location: String,
    val outEdge: Int,
    val sensorId: String,
    val spot: String,
    @Indexed(unique = true)
    val apMac: String,
    val tags: Map<String, String> = emptyMap()
) {
    fun presence(power: Int) = when {
        power >= inEdge -> Position.IN
        power >= limitEdge -> Position.LIMIT
        power >= outEdge -> Position.OUT
        else -> Position.NO_POSITION
    }
}

