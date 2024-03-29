package com.esmartit.seendevicesdatastore.infrastructure

import com.mongodb.reactivestreams.client.MongoClient
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.mongo.reactivestreams.ReactiveStreamsMongoLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class ShedLockConfig {

    @Bean
    fun lockProvider(
        mongo: MongoClient,
        @Value("\${MONGODB_SCHEMA_NAME:smartpoke}") databaseName: String
    ): LockProvider {
        return ReactiveStreamsMongoLockProvider(mongo.getDatabase(databaseName))
    }
}