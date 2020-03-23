package com.esmartit.seendevicesdatastore

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceStatRepository : ReactiveMongoRepository<SensorActivity, String>