package io.c8y.api.management.realtime

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono



typealias Message = Map<String, Any>
val Message.channel:String get() {
    return this["channel"] as String
}
val Message.data:Map<String,Any> get() {
    return this["data"] as Map<String, Any>
}

typealias ChannelId = String

interface Realtime {
    fun subscribe(channelId: List<ChannelId>): Flux<Message>
    fun subscirbe(channelId: ChannelId): Flux<Message>
    fun handshake(): Mono<Void>
    fun disconnect(): Mono<Void>
}