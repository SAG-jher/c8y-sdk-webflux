package io.c8y.api

import io.c8y.api.inventory.InventoryApi
import io.c8y.api.inventory.alarm.AlarmApi
import io.c8y.api.inventory.devicecontrol.DeviceControlApi
import io.c8y.api.inventory.event.EventApi
import io.c8y.api.inventory.measurement.MeasurementApi
import io.c8y.api.management.application.ApplicationApi
import io.c8y.api.management.cep.CepApi
import io.c8y.api.management.cep.SmartRuleApi
import io.c8y.api.management.tenant.TenantApi
import io.c8y.api.management.tenant.UserApi
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono

class RestApi(
    val baseUrl: UriBuilderFactory,
    val client: WebClient,
    val websocket: WebSocketConnect,
    val tenant: Mono<String>,
    private val tenantDomainSupport: Boolean
) {

    fun tenant(): TenantApi {
        return TenantApi(client, baseUrl, tenant, tenantDomainSupport);
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
        return SmartRuleApi(baseUrl, client)
    }

    fun alarm(): AlarmApi {
        return AlarmApi(baseUrl, client)
    }
}

class RestException(message: String, val status: HttpStatus) : RuntimeException(message)

