package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Document
data class DeviceWithPosition(
    val id: String? = null,
    val macAddress: String,
    val seenTime: Instant,
    val position: Position
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

    fun findBySeenTimeBetween(start: Instant, end: Instant): Flux<DeviceWithPosition>
}

@Repository
interface DevicePositionRepository : MongoRepository<DeviceWithPosition, String> {
    fun findByMacAddressAndSeenTime(macAddress: String, seenTime: Instant): DeviceWithPosition?
}