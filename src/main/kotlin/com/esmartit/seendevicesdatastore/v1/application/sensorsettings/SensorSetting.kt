package com.esmartit.seendevicesdatastore.v1.application.sensorsettings

import com.esmartit.seendevicesdatastore.domain.Position
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@Document
data class SensorSetting(
    val id: String? = null,
    val inEdge: Int,
    val limitEdge: Int,
    val location: String,
    val outEdge: Int,
    val sensorId: String,
    val spot: String,
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

@RepositoryRestResource(collectionResourceRel = "sensor-settings", path = "sensor-settings")
interface SensorSettingRepository : MongoRepository<SensorSetting, String> {
    fun findByApMac(apMac: String): SensorSetting?
}
