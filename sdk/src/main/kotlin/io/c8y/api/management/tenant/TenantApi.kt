package io.c8y.api.management.tenant

import io.c8y.api.management.application.Application
import io.c8y.api.management.application.ApplicationReference
import io.c8y.api.management.application.ApplicationReferenceCollection
import io.c8y.api.support.Page
import io.c8y.api.support.Pageable
import io.c8y.api.support.Paging
import io.c8y.api.support.handleRestError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

data class Tenant(
    val id: String? = null,
    val domain: String? = null,
    val company: String = id!!,
    val adminName: String = "admin",
    val adminEmail: String = "$adminName@cumulocity.com",
    val adminPass: String = "q1w2e3r4Q!W@E#R$",
    val sendPasswordResetEmail: Boolean = false,
    val storageLimitPerDevice: Long = 0
)

data class TenantReference(val tenant: Tenant)

data class TenantCollection(val tenants: Iterable<Tenant>, override val statistics: Page) :
    Pageable<Tenant> {
    override fun iterator(): Iterator<Tenant> {
        return tenants.iterator()
    }
}



class TenantApi(
    private val client: WebClient,
    private val baseUrl: UriBuilderFactory,
    private val tenant: Mono<String>,
    private val tenantDomainSupport: Boolean
) {

    fun list(vararg params: Pair<String, Any>): Flux<Tenant> {
        return Paging(client, path = "tenant/tenants")
            .list<Tenant, TenantCollection>(params)

    }

    fun get(id: String): Mono<Tenant> {
        return client.get()
            .uri { uri -> uri.path("tenant/tenants/{id}").build(id) }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Tenant::class.java)
    }


    fun options(): OptionApi {
        return OptionApi(client.mutate().baseUrl(baseUrl.builder().path("/tenant/options").build().toString()).build())
    }
    fun systemOptions(): OptionApi {
        return OptionApi(client.mutate().baseUrl(baseUrl.builder().path("/tenant/system/options").build().toString()).build())
    }

    fun subscribe(application: String): Mono<Void> {
        return tenant.flatMap {
            subscribe(it, application)
        }
    }

    fun subscribe(tenant: String, application: String): Mono<Void> {
        return client.post().uri { uri -> uri.path("tenant/tenants/{id}/applications").build(tenant) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Mono.just(
                    ApplicationReference(
                        application = Application(
                            id = application
                        )
                    )
                ),
                ApplicationReference::class.java
            )
            .retrieve()
            .handleRestError()
            .bodyToMono(Void::class.java)
    }

    fun isSubscribed(tenant: String, application: String): Mono<Boolean> {
        return Paging(client, path = "tenant/tenants/$tenant/applications")
            .list<ApplicationReference, ApplicationReferenceCollection>()
            .any {
                it.application.id.equals(application)
            }
    }

    fun unsubscribe(application: String): Mono<Void> {
        return tenant.flatMap {
            unsubscribe(it, application)
        }
    }

    fun unsubscribe(tenant: String, application: String): Mono<Void> {
        return client.delete()
            .uri { uri -> uri.path("tenant/tenants/{id}/applications/{app}").build(tenant, application) }
            .retrieve()
            .handleRestError()

            .bodyToMono(Void::class.java)
    }

    fun create(tenant: Tenant): Mono<Tenant> {
        return currentTenant().get().map { currentTenant ->
            currentTenant.domainName
        }.flatMap { baseDomain ->
            client.post()

                .uri { uri -> uri.path("tenant/tenants").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    Mono.just(
                        tenant.copy(
                            domain = if (tenantDomainSupport) domainForTenant(
                                baseUrl.builder().build().toString(),
                                tenant.id!!
                            ) else {
                                domainForTenant(
                                    baseDomain!!,
                                    tenant.id!!
                                )
                            }
                        )
                    ), Tenant::class.java
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .handleRestError()
                .bodyToMono(Tenant::class.java)
        }


    }

    fun delete(id: String): Mono<Void> {
        return client.delete()
            .uri { uri -> uri.path("tenant/tenants/{id}").build(id) }
            .retrieve()
            .handleRestError()
            .bodyToMono(Void::class.java)
    }

    fun currentTenant(): CurrentTenantApi {
        return CurrentTenantApi(client)
    }

    fun update(id: String?,vararg update:Pair<String,Any?>): Mono<Tenant> {
        return client.put()
            .uri { uri -> uri.path("tenant/tenants/{id}").build(id) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Mono.just(
                    mapOf(*update)
                ), Map::class.java
            )
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Tenant::class.java)
    }

}


 fun domainForTenant(url: String, id: String, tenantDomainSupport: Boolean = true): String {
    if (!tenantDomainSupport) {
        return URI.create(url).host
    }
    val host = if (url.contains("://")) URI.create(url).host else url
    return listOf(id)
        .union(
            host
                .split(".")
                .drop(1)
        )
        .joinToString(separator = ".")

}