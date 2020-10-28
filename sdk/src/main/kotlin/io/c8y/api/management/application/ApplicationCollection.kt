package io.c8y.api.management.application

import io.c8y.api.support.Page
import io.c8y.api.support.Pageable

data class ApplicationCollection(val applications: Iterable<Application>, override val statistics: Page) :
    Pageable<Application> {
    override fun iterator(): Iterator<Application> {
        return applications.iterator()
    }
}