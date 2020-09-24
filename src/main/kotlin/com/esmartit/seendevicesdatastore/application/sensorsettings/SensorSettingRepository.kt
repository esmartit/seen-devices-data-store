package com.esmartit.seendevicesdatastore.application.sensorsettings

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(collectionResourceRel = "sensor-settings", path = "sensor-settings")
interface SensorSettingRepository : MongoRepository<SensorSetting, String> {
    fun findByApMac(apMac: String): SensorSetting?
}