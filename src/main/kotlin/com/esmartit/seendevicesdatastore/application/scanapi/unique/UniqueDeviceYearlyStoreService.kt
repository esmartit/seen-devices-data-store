package com.esmartit.seendevicesdatastore.application.scanapi.unique

import com.esmartit.seendevicesdatastore.domain.UniqueDeviceYearly
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface UniqueDeviceYearlyRepository : MongoRepository<UniqueDeviceYearly, String> {
    fun findByClientMacAndYearSeenTime(clientMac: String, yearSeenTime: Int): UniqueDeviceYearly?
}
