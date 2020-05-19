package com.esmartit.seendevicesdatastore.changelogs

import com.github.mongobee.Mongobee
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class MongoBeeConfig {

    @Bean
    fun mongobee(@Value("\${spring.data.mongodb.uri}") mongoDbUri: String): Mongobee {
        val runner = Mongobee(mongoDbUri)
        runner.setDbName("smartpoke")
        runner.setChangeLogsScanPackage("com.esmartit.seendevicesdatastore.changelogs")
        return runner
    }
}
