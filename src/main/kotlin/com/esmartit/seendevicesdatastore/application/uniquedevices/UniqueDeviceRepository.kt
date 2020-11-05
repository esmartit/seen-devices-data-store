package com.esmartit.seendevicesdatastore.application.uniquedevices

import com.esmartit.seendevicesdatastore.domain.UniqueDevice
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UniqueDeviceRepository : MongoRepository<UniqueDevice, String> {
//    fun findByClientMac(clientMac: String): UniqueDevice?
}