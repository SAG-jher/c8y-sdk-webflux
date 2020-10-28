package io.c8y.api.support

interface Pageable<T> : Iterable<T> {
    val statistics: Page?
}