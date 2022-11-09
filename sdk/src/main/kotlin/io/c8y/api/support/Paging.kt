package io.c8y.api.support

import com.google.common.net.UrlEscapers
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.temporal.TemporalAccessor

class Paging(private val client: WebClient, private val path: String) {

    var log = loggerFor<Paging>()

    inline fun <E, reified T : Pageable<E>> list(params: Array<out Pair<String, Any>> = arrayOf()): Flux<E> {
        return doList(params, T::class.java)
    }

    fun <E, C : Pageable<E>> getPage(
        index: Int,
        pageSize: Int = 2000,
        collectionType: Class<C>,
        vararg params: Pair<String, Any>
    ): Mono<C> {

        return client.get().uri { uri ->
            uri.path(path).apply<UriBuilder> {
                params.forEach { query ->
                    when(query.first) {
                        "q","query"->{
                            this.queryParam(query.first, query.second)
                        }
                        else-> this.queryParam(query.first, "{${query.first}}")
                    }
                }
            }
                .apply<UriBuilder> {
                    if (!params.any { it.first == "pageSize" }) {
                        this.queryParam("pageSize", pageSize)
                    }
                }
                .queryParam("currentPage", index)
                .build(mapOf(*params).mapValues<String?, Any, Any?> {
                    when (it.value) {
                        is TemporalAccessor -> (it.value as TemporalAccessor).asQueryParam()
                        else -> it.value
                    }
                })
        }

            .retrieve()
            .handleRestError()
            .bodyToMono(collectionType)


    }

    fun <E, C : Pageable<E>> doList(params: Array<out Pair<String, Any>>, collectionType: Class<C>): Flux<E> {


        return Flux.create { emitter ->
            var currentPage = 1;
            var leftToEmit: Long = 0;
            var isFetching: Boolean = false;


            fun emit(retry: Int = 0) {
                if (!emitter.isCancelled) {
                    isFetching = true;
                    getPage(index = currentPage, collectionType = collectionType, params = params)
                        .doOnSuccess {
                            if (it.any() && leftToEmit > 0) {
                                it.forEach { t ->
                                    --leftToEmit
                                    if (t == null) {
                                        log.warn("Null item emited for {} and {}", path, params)
                                    }
                                    emitter.next(t!!)
                                }
                                ++currentPage
                                emit()
                            } else {
                                if (!it.any()) {
                                    emitter.complete()
                                }
                                isFetching = false;
                            }
                        }
                        .doOnError {
                            if (retry < 3) {
                                emit(retry + 1)
                            } else {
                                emitter.error(it)
                            }
                        }
                        .subscribe()
                }
            }

            emitter.onRequest { reqested ->
                leftToEmit += reqested
                if (!isFetching) {
                    emit()
                }
            }

        }
    }

    inline fun <E, reified T : Pageable<E>> count(params: Array<out Pair<String, Any>>): Mono<Long?> {
        return getPage(1, pageSize = 1, T::class.java, *params, "withTotalPages" to "true")
            .map {
                it.statistics?.totalElements()
            }
    }
}