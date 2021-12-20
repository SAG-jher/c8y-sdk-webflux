package io.c8y.api.management.realtime

import com.fasterxml.jackson.databind.ObjectMapper
import io.c8y.api.BasicCredentials
import io.c8y.api.PlatformApi
import io.c8y.api.support.loggerFor
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.core.scheduler.Schedulers
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


class RealtimeWebsocketFlux(val c8y: PlatformApi, val path: String) : Realtime {
    val log = loggerFor<RealtimeWebsocketFlux>()

    companion object {
        internal val scheduler = Schedulers.newBoundedElastic(100, 1000, "realtime")
    }

    val id: AtomicLong = AtomicLong(0)
    lateinit var clientId: String;
    val mapper = ObjectMapper()
    var session: WebSocketSession? = null
    var lastPing: Long = 0
    lateinit var pingScheduler: Disposable


    val subscriptions =
        ConcurrentHashMap<ChannelId, MutableList<FluxSink<Message>>>()

    override fun subscribe(channelIds: List<ChannelId>): Flux<Message> {
        return channelIds.fold(Flux.empty()) { acc, s -> acc.mergeWith(this.subscirbe(s)) }
    }


    override fun subscirbe(channelId: ChannelId): Flux<Message> {
        return Flux.create<Message> { sink ->
            log.debug("Subscribe to {}", channelId)
            val subscribes = subscriptions.getOrPut(channelId, ::CopyOnWriteArrayList)!!
            subscribes.add(sink)

            if (!channelId.startsWith("/meta/")) {
                subscirbe("/meta/subscribe").takeUntil { subscription ->
                    if (subscription["subscription"]?.equals(channelId)!!) {
                        if (!(subscription["successful"] as Boolean)) {
                            sink.error(java.lang.IllegalStateException(subscription["error"] as String))
                            subscribes.remove(sink)
                        }
                    }
                    return@takeUntil true
                }

                send(
                    mapOf(
                        "channel" to "/meta/subscribe",
                        "clientId" to clientId,
                        "subscription" to channelId

                    )
                )
                    .subscribeOn(scheduler)
                    .subscribe()
            }

            sink.onCancel { subscribes.remove(sink) }
            sink.onDispose { subscribes.remove(sink) }

        }
    }

    private val pingTimeout = Duration.ofSeconds(10)

    override fun handshake(): Mono<Void> {
        if (session != null) {
            throw IllegalStateException("already connected");
        }

        return Mono.create<Void> { handshakeSink ->

            c8y.websocket().connect({ uri ->
                uri.path(path).build()
            }, object : WebSocketHandler {

                override fun handle(session: WebSocketSession): Mono<Void> {
                    this@RealtimeWebsocketFlux.session = session
                    this@RealtimeWebsocketFlux.lastPing = Clock.systemDefaultZone().millis();
                    log.debug("Session {}", session)
                    this@RealtimeWebsocketFlux.pingScheduler = startPingScheduler(session)


                    doHandshake(handshakeSink)
                    subscirbe("/meta/connect").subscribeOn(scheduler).subscribe {
                        send(
                            mapOf(
                                "channel" to "/meta/connect",
                                "connectionType" to "websocket",
                                "clientId" to clientId
                            )
                        ).subscribe()
                    }





                    return send(
                        createHandshakeMessage()
                    ).and(session.receive()
                        .subscribeOn(scheduler)
                        .doOnNext {
                            log.debug("Recevied {}", it)
                        }
                        .filter {
                            onWebSocketMessage(it)
                        }
                        .map { it.payloadAsText }
                        .concatMap { message ->
                            onMessage(message)
                        }
                        .doOnError {
                            log.error("Failure ", it)
                        }
                    )
                }


            }).subscribe()


        }


    }

    private fun doHandshake(handshakeSink: MonoSink<Void>) {
        subscirbe("/meta/handshake").take(1).single().subscribeOn(scheduler).subscribe { message ->
            if (message["successful"] as Boolean) {
                clientId = message["clientId"] as String
                send(
                    mapOf(
                        "channel" to "/meta/connect",
                        "connectionType" to "websocket",
                        "clientId" to clientId,
                        "advice" to mapOf(
                            "timeout" to 0
                        )
                    )
                ).doOnSuccess {
                    handshakeSink.success()
                }.subscribe()
            } else {
                handshakeSink.error(IllegalStateException(message["error"] as String))
            }
        }
    }

    private fun onWebSocketMessage(it: WebSocketMessage): Boolean {
        return when (it.type) {
            WebSocketMessage.Type.PONG -> {
                log.debug("Received pong {}", it)
                lastPing = Clock.systemDefaultZone().millis()
                false
            }
            else -> {
                true
            }
        }
    }

    private fun startPingScheduler(session: WebSocketSession): Disposable {
        return scheduler
            .schedulePeriodically(
                {
                    if (isPongTimeoutExceeded()) {
                        log.error("No response to ping")
                        this@RealtimeWebsocketFlux.disconnect().subscribe()
                    }

                    send(Mono.just(session.pingMessage {
                        it.allocateBuffer()
                    })).subscribe()

                    lastPing

                }, pingTimeout.toMillis(), pingTimeout.toMillis(),
                TimeUnit.MILLISECONDS
            )
    }

    private fun isPongTimeoutExceeded(): Boolean {
        val diff = Clock.systemDefaultZone().millis() - lastPing
        return diff >= 0 && Duration.ofMillis(
            diff
        )
            .compareTo(pingTimeout) >= 0
    }

    private fun onMessage(data: String): Mono<Any> {
        log.debug("Received message {}", data)
        val deseralized = mapper.readerFor(Map::class.java).readValues<Map<String, Any>>(data).readAll()
        log.debug("Data {}", deseralized)
        return Flux.fromIterable(deseralized)
            .doOnNext { message ->
                val channel = message["channel"] as ChannelId
                val parts = channel.split('/')

                (0..parts.size - 1).forEach { current ->
                    val wildcard = parts.subList(0, current).joinToString("/")
                    dispatch(wildcard + "/**", message)
                    if (current == parts.size - 1) {
                        dispatch(wildcard + "/*", message)
                    }
                }
                dispatch(channel, message)
            }
            .collectList()
            .map { it as Any }

    }

    private fun dispatch(key: String, message: Map<String, Any>?) {
        log.trace("dispatch to channel {} of {}", key, message)
        subscriptions.get(key)?.forEach { sink ->
            sink.next(message)
        }
    }

    private fun createHandshakeMessage(): Map<String, Any> {
        return mapOf(
            "id" to id.incrementAndGet().toString(),
            "ext" to mapOf(
                "com.cumulocity.authn" to mapOf(
                    "token" to c8y.credentials.let {
                        when (it) {
                            is BasicCredentials -> {
                                it.encode()
                            }
                            else -> {
                                throw IllegalStateException("un supported")

                            }
                        }
                    }
                )
            ),
            "version" to "1.0",
            "minimumVersion" to "1.0",
            "channel" to "/meta/handshake",
            "supportedConnectionTypes" to listOf("websocket"),
            "advice" to mapOf("timeout" to 60000, "interval" to 0)
        )
    }

    override fun disconnect(): Mono<Void> {
        return Mono.empty()
    }

    private fun send(data: Any): Mono<Void> {
        log.debug("Sending data {} ", data)
        return send(
            Mono.just(data)
                .map { if (data is WebSocketMessage) data else session!!.textMessage(mapper.writeValueAsString(data)) })

    }

    private fun send(data: Mono<WebSocketMessage>): Mono<Void> {
        return session!!.send(data.doOnNext {
            log.debug("Sending message {}", it)
        })

    }
}
