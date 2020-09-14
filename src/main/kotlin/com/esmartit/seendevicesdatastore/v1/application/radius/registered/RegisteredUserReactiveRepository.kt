package com.esmartit.seendevicesdatastore.v1.application.radius.registered

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.stream.Stream

//@Repository
//@RepositoryRestResource(collectionResourceRel = "registered-users", path = "registered-users")
//interface RegisteredUserReactiveRepository : ReactiveMongoRepository<RegisteredUser, String> {
//    fun findByInfoSeenTimeGreaterThanEqual(startOfDay: Instant): Flux<RegisteredUser>
//}

@RepositoryRestResource(collectionResourceRel = "registered-users", path = "registered-users")
interface RegisteredUserRepository : MongoRepository<RegisteredUser, String> {
    fun findByInfoUsername(username: String): RegisteredUser?
    fun findByInfoClientMac(clientMac: String): List<RegisteredUser>
    fun findByInfoSeenTimeGreaterThanEqual(startOfDay: Instant): Stream<RegisteredUser>
}