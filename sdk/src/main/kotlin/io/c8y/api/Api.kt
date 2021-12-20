package io.c8y.api

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface GetById<T>{
    fun get(id:String): Mono<T>
}

interface Listable<T>{
    fun list(vararg params: Pair<String, Any>): Flux<T>
}

interface Creatable<T> {
    fun create(ob: T): Mono<T>
}