package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.dsl.IntegrationFlow
import java.util.concurrent.CompletableFuture

@Configuration
class FlowConfig {

    @Bean
    fun scanApiFlow(scanApiStoreService: ScanApiStoreService): IntegrationFlow {
        return IntegrationFlow { flow ->
            flow.handle { message ->
                CompletableFuture.runAsync {
                    val scanApiActivity = message.payload as ScanApiActivity
                    scanApiStoreService.save(scanApiActivity).subscribe()
                }
            }
        }
    }
}