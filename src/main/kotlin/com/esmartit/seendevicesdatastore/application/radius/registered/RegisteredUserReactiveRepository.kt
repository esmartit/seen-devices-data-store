package com.esmartit.seendevicesdatastore.application.radius.registered

import com.esmartit.seendevicesdatastore.domain.RegisteredUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.stream.Stream

@Repository
//@RepositoryRestResource(collectionResourceRel = "registered-users", path = "registered-users")
interface RegisteredUserReactiveRepository : ReactiveMongoRepository<RegisteredUser, String> {
    fun findByInfoSeenTimeGreaterThanEqual(startOfDay: Instant): Flux<RegisteredUser>
}

@Repository
//@RepositoryRestResource(collectionResourceRel = "registered-users", path = "registered-users")
interface RegisteredUserRepository : MongoRepository<RegisteredUser, String> {
    fun findByInfoUsername(username: String): RegisteredUser?
    fun findByInfoClientMac(clientMac: String): List<RegisteredUser>
    fun findByInfoSeenTimeGreaterThanEqual(startOfDay: Instant): Stream<RegisteredUser>
}