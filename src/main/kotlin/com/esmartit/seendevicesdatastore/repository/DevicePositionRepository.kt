package com.esmartit.seendevicesdatastore.repository

import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Document
@CompoundIndex(def = "{'macAddress':1, 'seenTime':1}", name = "device_with_position_macAddress_seenTime_idx")
data class DeviceWithPosition(
    val id: String? = null,
    val macAddress: String,
    @Indexed(name = "seenTime_idx")
    val seenTime: Instant,
    val position: Position,
    val countInAnHour: Int = 0,
    val userInfo: RadiusActivity? = null,
    @Indexed(name = "lastUpdate_idx")
    val lastUpdate: Instant = seenTime
) {
    fun isWithinRange(): Boolean {
        return position != Position.NO_POSITION
    }
}

enum class Position(val value: Int) {
    IN(3), LIMIT(2), OUT(1), NO_POSITION(-1)
}

@Repository
interface DevicePositionReactiveRepository : ReactiveMongoRepository<DeviceWithPosition, String> {

    fun findBySeenTimeGreaterThanEqual(time: Instant): Flux<DeviceWithPosition>
    fun findByLastUpdateGreaterThanEqual(time: Instant): Flux<DeviceWithPosition>
}

@Repository
interface DevicePositionRepository : MongoRepository<DeviceWithPosition, String> {
    fun findByMacAddressAndSeenTime(macAddress: String, seenTime: Instant): DeviceWithPosition?
}