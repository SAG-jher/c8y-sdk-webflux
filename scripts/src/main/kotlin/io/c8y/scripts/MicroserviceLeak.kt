package io.c8y.scripts

import io.c8y.api.PlatformApi
import io.c8y.api.management.tenant.Tenant
import io.c8y.api.management.tenant.TenantApi
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.TimeUnit


fun main() {
    val local = Platform["local"]
    val rest = local.rest()
    val tenant = createTenant(rest.tenant(), "jaro-0")
        .block()

    executePerformanceTest(tenant,local)

    Thread.sleep(100000)
}

fun executePerformanceTest(tenant: Tenant, platform: PlatformApi) {
    val platform = platform.forTenant(tenant)
    val echo = platform.rest().application().clientFor("cep")

    Schedulers.boundedElastic()
        .schedulePeriodically({
            Flux.range(0,5)
                .flatMap {
                    echo.get()
                        .uri{ uri -> uri.path("cep/health").build()}
                        .exchange()

                }
                .timeout(Duration.ofSeconds(5L))
                .doOnError {
                   log.error("failed",it)
                }
                .subscribe {
                    log.info("Recevied {}",it.statusCode().value())
                }

        },0,500,TimeUnit.MILLISECONDS)


}


private fun createTenant(tenantApi: TenantApi, id: String): Mono<Tenant> {
    return tenantApi.get(id).onErrorResume {
        tenantApi.create(
            Tenant(
                id = id
            )
            )
    }
}