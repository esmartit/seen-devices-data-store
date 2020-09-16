package com.esmartit.seendevicesdatastore.application.radius.online

import com.esmartit.seendevicesdatastore.domain.RadiusActivity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface RadiusActivityReactiveRepository : ReactiveMongoRepository<RadiusActivity, String>

@Repository
interface RadiusActivityRepository : MongoRepository<RadiusActivity, String> {
    fun findByInfoUsername(username: String): RadiusActivity?
    fun findByInfoCallingStationId(clientMac: String): RadiusActivity?

    @Query("{'info.callingStationId' : ?0}, {\$limit : 1}", sort = "{'info.eventTimeStamp' : -1 }")
    fun findLastByClientMac(clientMac: String): RadiusActivity?


    @Query("{'info.callingStationId' : ?0}, {\$limit : 1}", sort = "{'info.eventTimeStamp' : -1 }")
    fun findLastByClientMac(clientMac: String, pageable: Pageable): Page<RadiusActivity>

    fun findFirstByInfoCallingStationIdOrderByInfoEventTimeStampDesc(clientMac: String): RadiusActivity?
}