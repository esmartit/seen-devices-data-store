package com.esmartit.seendevicesdatastore.v1.application.sensorsettings

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.rest.core.annotation.HandleAfterCreate
import org.springframework.data.rest.core.annotation.HandleAfterSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

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
)

@RepositoryRestResource(collectionResourceRel = "sensor-settings", path = "sensor-settings")
interface SensorSettingRepository : MongoRepository<SensorSetting, String> {
    fun findByApMac(apMac: String): SensorSetting?
}

@Component
@RepositoryEventHandler
class SensorSettingEventHandler(private val sensorSettingProducer: SensorSettingProducer) {

    @HandleAfterSave
    @HandleAfterCreate
    fun handleBookBeforeCreate(sensorSetting: SensorSetting) {
        sensorSettingProducer.process(sensorSetting)
    }
}

@EnableBinding(SensorSettingSource::class)
class SensorSettingProducer(private val source: SensorSettingSource) {
    fun process(event: SensorSetting) {
        event.run {
            source.output().send(
                MessageBuilder
                    .withPayload(this)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, this.sensorId.toByteArray())
                    .build()
            )
        }
    }
}

interface SensorSettingSource {

    @Output(SENSOR_SETTINGS_OUTPUT)
    fun output(): MessageChannel

    companion object {
        const val SENSOR_SETTINGS_OUTPUT = "sensor-settings-output"
    }
}
