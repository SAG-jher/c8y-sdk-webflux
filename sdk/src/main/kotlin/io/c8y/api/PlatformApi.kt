package io.c8y.api

import com.google.common.base.Stopwatch
import com.google.common.io.BaseEncoding
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder
import com.hivemq.client.mqtt.mqtt3.reactor.Mqtt3ReactorClient
import io.c8y.api.management.tenant.Tenant
import io.c8y.api.management.tenant.domainForTenant
import io.c8y.api.support.Dynamic
import io.c8y.api.support.loggerFor
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.util.concurrent.TimeUnit


interface WebSocketConnect {
    fun connect(uri: (UriBuilder) -> URI, handler: WebSocketHandler): Mono<Void>
}


interface Credentials {
    val tenant: String?
}


data class BasicCredentials(
    val username: String,
    val password: String
) : Credentials {

    override val tenant: String?
        get() {
            return (if (username.contains('/')) {
                username.subSequence(0, username.indexOf('/'))
            } else {
                null
            }) as String?
        }

    fun encode(): String {
        return BaseEncoding.base64().encode("$username:$password".toByteArray())
    }
}

private val wsClient = ReactorNettyWebSocketClient()


private val sslContext = SslContextBuilder
    .forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE)
    .build();
private val httpConnector: ClientHttpConnector =
    ReactorClientHttpConnector(HttpClient.create().secure { t -> t.sslContext(sslContext) }.compress(true))

private val log = loggerFor<PlatformApi>()


private val restClient = WebClient.builder()
    .exchangeStrategies(
        ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs()
                    .maxInMemorySize(-1)
            }
            .build()
    )
    .filter { req, next ->
        val stopwatch = Stopwatch.createStarted()
        next.exchange(req)
            .doOnEach { signal ->

                if (signal.isOnError) {
                    stopwatch.stop()
                    log.info("Request failed {} {}, took: {}", req.method(), req.url(), signal.throwable, stopwatch)
                } else if (signal.isOnNext) {
                    stopwatch.stop()
                    log.info(
                        "Request {} {} => {}, took: {}",
                        req.method(),
                        req.url(),
                        signal.get()?.statusCode(),
                        stopwatch
                    )
                }
            }
    }
    .clientConnector(httpConnector)
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

    .build();


class PlatformApi(
    val config: Config,
    val credentials: Credentials,
    private val baseUrl: String,
    private val tenantDomainSupport: Boolean = true
) {
    val url: UriBuilder
        get() {
            return DefaultUriBuilderFactory().uriString(baseUrl)
        }
    val tenant: Mono<String> by lazy {
        Mono.defer { rest().tenant().currentTenant().get().map { it.name!! } }.cache()
    }


    fun withBaseUrl(baseUrl: String): PlatformApi {
        return PlatformApi(
            config = config,
            credentials = credentials,
            baseUrl = baseUrl,
            tenantDomainSupport = tenantDomainSupport
        )
    }

    fun forTenant(
        tenant: Tenant,
        baseUrl: String = this.baseUrl,
        useHttps: Boolean = config.getOrDefault("useHttps", false)
    ): PlatformApi {
        val currentBase = url.build()
        return PlatformApi(
            config = config,
            baseUrl = DefaultUriBuilderFactory().uriString(baseUrl).host(
                tenantDomain(
                    baseUrl,
                    tenant.id!!
                )
            ).apply {
                if (useHttps) {
                    this.port(443).scheme("https")
                } else {

                    this.port(currentBase.port).scheme(currentBase.scheme)
                }
            }.build().toString(),
            credentials = BasicCredentials(
                username = tenant.id + "/admin",
                password = "q1w2e3r4Q!W@E#R$"
            )
        )
    }

    private fun tenantDomain(url: String, id: String): String {
        return domainForTenant(url, id, tenantDomainSupport)

    }

    fun mqtt3(
        clientId: String,
        credentials: Credentials = this.credentials,
        port: Int = 8883,
        configure: (Mqtt3ClientBuilder) -> Unit = {}
    ): Mqtt3ReactorClient {


        return Mqtt3ReactorClient.from(
            MqttClient.builder()
                .useMqttVersion3()
                .transportConfig()
                .socketConnectTimeout(120, TimeUnit.SECONDS)
                .serverPort(port)
                .serverHost(
                    DefaultUriBuilderFactory().uriString(baseUrl)
                        .scheme("tcp")
                        .build().host
                )
                .sslConfig()
                .handshakeTimeout(60, TimeUnit.SECONDS)
                .applySslConfig()
                .applyTransportConfig()
                .automaticReconnect()
                .initialDelay(1, TimeUnit.SECONDS)
                .maxDelay(10, TimeUnit.SECONDS)
                .applyAutomaticReconnect()
                .identifier(clientId)
                .simpleAuth()
                .let {
                    when (credentials) {
                        is BasicCredentials -> {
                            it.username(credentials.username)
                                .password(credentials.password.toByteArray())

                        }
                        else -> throw IllegalStateException("unsupported credentials " + credentials)
                    }
                }
                .applySimpleAuth()
                .apply(configure)
                .build())


    }

    fun websocket(): WebSocketConnect {
        val uriBuilder = DefaultUriBuilderFactory(baseUrl)
        return object : WebSocketConnect {
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
                                throw  IllegalStateException("is not supported $credentials")
                            }
                        }
                    }, handler
                )
            }
        }
    }

    fun rest(): RestApi {

        val uriBuilder = DefaultUriBuilderFactory(baseUrl)

        return RestApi(
            baseUrl = uriBuilder,
            client = restClient.mutate()
                .baseUrl(baseUrl).run {
                    when (credentials) {
                        is BasicCredentials -> filter(
                            ExchangeFilterFunctions.basicAuthentication(
                                credentials.username,
                                credentials.password
                            )
                        )
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
                                    throw  IllegalStateException("is not supported $credentials")
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

    fun withCredentials(credentials: Credentials): PlatformApi {
        return PlatformApi(config = config, credentials, baseUrl, tenantDomainSupport)
    }
}


data class Identity(
    val externalId: String? = null,
    val type: String? = null
) : Dynamic<Identity>()


