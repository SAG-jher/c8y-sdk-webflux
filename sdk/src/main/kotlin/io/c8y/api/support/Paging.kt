package io.c8y.api.support

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

class Paging(private val client: WebClient, private val path: String) {

    var log = loggerFor<Paging>()

    inline fun <E, reified T : Pageable<E>> list(params: Array<out Pair<String, Any>> = arrayOf()): Flux<E> {
        return doList(params, T::class.java)
    }

    fun <E, C : Pageable<E>> doList(params: Array<out Pair<String, Any>>, collectionType: Class<C>): Flux<E> {
        fun encodeDateTime(dateTime: TemporalAccessor) =
            when (dateTime) {
                is ZonedDateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                    LocalDateTime.ofInstant(
                        dateTime.toInstant(),
                        ZoneOffset.UTC
                    ).atZone(ZoneOffset.UTC)
                )
                else -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime)
            }


        fun getPage(index: Int, pageSize: Int = 2000): Mono<C> {
            return client.get().uri { uri ->
                uri.path(path).apply {
                    params
                        .forEach { query ->
                            queryParam(
                                query.first, when (query.second) {
                                    is TemporalAccessor -> encodeDateTime(query.second as TemporalAccessor)
                                    else -> query.second
                                }
                            )

                        }
                }
                    .apply {
                        if (!params.any { it.first == "pageSize" }) {
                            queryParam("pageSize", pageSize)
                        }
                    }
                    .queryParam("currentPage", index).build()
            }

                .retrieve()
                .handleRestError()
                .bodyToMono(collectionType)


        }


        return Flux.create { emitter ->
            var currentPage = 1;
            var leftToEmit: Long = 0;
            var isFetching: Boolean = false;


            fun emit(retry: Int = 0) {
                if (!emitter.isCancelled) {
                    isFetching = true;
                    getPage(currentPage)
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
                                isFetching=false;
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
                if(!isFetching) {
                    emit()
                }
            }

        }
    }
}