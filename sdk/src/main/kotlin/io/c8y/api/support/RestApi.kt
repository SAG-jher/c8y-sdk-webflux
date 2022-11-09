package io.c8y.api

import io.c8y.api.inventory.InventoryApi
import io.c8y.api.inventory.alarm.AlarmApi
import io.c8y.api.inventory.devicecontrol.DeviceControlApi
import io.c8y.api.inventory.event.EventApi
import io.c8y.api.inventory.measurement.MeasurementApi
import io.c8y.api.inventory.smartrest.SmartrestApi
import io.c8y.api.management.application.ApplicationApi
import io.c8y.api.management.cep.CepApi
import io.c8y.api.management.cep.SmartRuleApi
import io.c8y.api.management.tenant.TenantApi
import io.c8y.api.management.tenant.UserApi
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono
import java.net.URI

object RestApis {
    fun create(
        baseUrl: String,
        client: WebClient,
        tenant: Mono<String>,
        tenantDomainSupport: Boolean,
        credentials: Credentials
    ): RestApi {
        val uriBuilder = DefaultUriBuilderFactory(baseUrl)

        return RestApi(
            baseUrl = uriBuilder,
            client = client.mutate()
                .baseUrl(baseUrl).run {
                    when (credentials) {
                        is BasicCredentials ->
                            if (credentials.hasToken()) {
                                filter { request, next ->
                                    next.exchange(
                                        ClientRequest.from(request)
                                            .headers { headers: HttpHeaders ->
                                                headers.setBearerAuth(credentials.token)
                                            }
                                            .build())
                                }

                            } else {
                                filter(
                                    ExchangeFilterFunctions.basicAuthentication(
                                        credentials.username,
                                        credentials.password
                                    )
                                )
                            }


                        else -> throw IllegalStateException("unsupported credentials " + credentials)
                    }

                }
                .build(),

            websocket = object : WebSocketConnect {
                override fun connect(uri: (UriBuilder) -> URI, handler: WebSocketHandler): Mono<Void> {

                    return wsClient.execute(
                        uri(uriBuilder.builder()),
                        HttpHeaders().run {
                            when (credentials) {
                                is BasicCredentials -> {
                                    setBasicAuth(credentials.username, credentials.password);
                                    this
                                }

                                else -> {
                                    throw IllegalStateException("is not supported $credentials")
                                }
                            }
                        }, handler
                    )
                }
            },
            tenant = tenant,
            tenantDomainSupport = tenantDomainSupport
        )

    }
}

class RestApi(
    val baseUrl: UriBuilderFactory,
    val client: WebClient,
    val websocket: WebSocketConnect,
    val tenant: Mono<String>,
    private val tenantDomainSupport: Boolean,
) {

    fun tenant(): TenantApi {
        return TenantApi(client, baseUrl, tenant, tenantDomainSupport);
    }
    fun oauth(): OAuthApi{
        return OAuthApi(client, baseUrl)
    }

    fun application(): ApplicationApi {
        return ApplicationApi(baseUrl, client)
    }

    fun inventory(): InventoryApi {
        return InventoryApi(client)
    }

    fun devicecontrol(): DeviceControlApi {
        return DeviceControlApi(client)
    }

    fun event(): EventApi {
        return EventApi(client)
    }

    fun measurement(): MeasurementApi {
        return MeasurementApi(client)
    }

    fun cep(): CepApi {
        return CepApi(client)
    }

    fun user(): UserApi {
        return UserApi(client, tenant = tenant().currentTenant().get().cache())
    }

    fun smartRules(): SmartRuleApi {
        return SmartRuleApi(client)
    }

    fun alarm(): AlarmApi {
        return AlarmApi(client)
    }

    fun smartrest(path: String = "s/"): SmartrestApi {
        return SmartrestApi(client, path)
    }

    fun websocket(): WebSocketConnect {
        return websocket
    }


}

data class ErrorDetails(
    val exceptionMessage: String,
    val exceptionClass: String?,
    val exceptionStackTrace: String?
)

data class RestError(val message: String, val info: String, val error: String, val details: ErrorDetails? = null)
class RestException(val error: RestError, val status: HttpStatus) : RuntimeException(error.message)

