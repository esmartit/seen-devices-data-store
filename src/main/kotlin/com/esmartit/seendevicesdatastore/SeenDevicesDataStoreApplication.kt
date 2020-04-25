package com.esmartit.seendevicesdatastore

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableMongoRepositories
class SeenDevicesDataStoreApplication

fun main(args: Array<String>) {
	runApplication<SeenDevicesDataStoreApplication>(*args)
}
