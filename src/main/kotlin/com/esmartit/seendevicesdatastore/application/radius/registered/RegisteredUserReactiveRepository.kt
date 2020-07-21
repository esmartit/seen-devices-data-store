package com.esmartit.seendevicesdatastore.application.radius.registered

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
interface RegisteredUserReactiveRepository : ReactiveMongoRepository<RegisteredUser, String> {
    fun findByInfoSeenTimeGreaterThanEqual(startOfDay: Instant): Flux<RegisteredUser>
}

@Repository
interface RegisteredUserRepository : MongoRepository<RegisteredUser, String> {
    fun findByInfoUsername(username: String): RegisteredUser?
    fun findByInfoClientMac(clientMac: String): List<RegisteredUser>
}