package com.esmartit.seendevicesdatastore.application.uniquedevices

import com.esmartit.seendevicesdatastore.domain.UniqueDevice
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UniqueDeviceReactiveRepository : ReactiveMongoRepository<UniqueDevice, String> {
//    fun findByClientMac(clientMac: String): Mono<UniqueDevice>
}