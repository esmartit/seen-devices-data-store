package com.esmartit.seendevicesdatastore

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.text.NumberFormat

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableMongoRepositories
class SeenDevicesDataStoreApplication

private val logger = LoggerFactory.getLogger(SeenDevicesDataStoreApplication::class.java)
fun main(args: Array<String>) {
    runApplication<SeenDevicesDataStoreApplication>(*args)

    val runtime = Runtime.getRuntime()

    val format: NumberFormat = NumberFormat.getInstance()

    val maxMemory = runtime.maxMemory()
    val allocatedMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val mb = 1024 * 1024
    val mega = " MB"

    logger.info("========================== Memory Info ==========================")
    logger.info("Free memory: " + format.format(freeMemory / mb).toString() + mega)
    logger.info("Allocated memory: " + format.format(allocatedMemory / mb).toString() + mega)
    logger.info("Max memory: " + format.format(maxMemory / mb).toString() + mega)
    logger.info(
		"Total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / mb).toString() + mega
	)
    logger.info("=================================================================\n")
}
