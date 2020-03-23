package com.esmartit.seendevicesdatastore

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories
class SeenDevicesDataStoreApplication

fun main(args: Array<String>) {
	runApplication<SeenDevicesDataStoreApplication>(*args)
}
