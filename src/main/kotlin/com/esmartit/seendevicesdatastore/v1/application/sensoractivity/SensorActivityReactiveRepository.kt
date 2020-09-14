package com.esmartit.seendevicesdatastore.v1.application.sensoractivity

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SensorActivityReactiveRepository : ReactiveMongoRepository<SensorActivity, String>

@Repository
interface SensorActivityRepository : MongoRepository<SensorActivity, String>