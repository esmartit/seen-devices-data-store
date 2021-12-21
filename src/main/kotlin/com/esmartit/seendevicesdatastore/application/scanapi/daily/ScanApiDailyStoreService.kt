package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityDaily
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ScanApiActivityDailyRepository : MongoRepository<ScanApiActivityDaily, String>
