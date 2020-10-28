package io.c8y.api.management.application

import io.c8y.api.support.Page
import io.c8y.api.support.Pageable

data class ApplicationReference(
    val application: Application
)

data class ApplicationReferenceCollection(
    val references: List<ApplicationReference>,
    override val statistics: Page
) :
    Pageable<ApplicationReference> {
    override fun iterator(): Iterator<ApplicationReference> {
        return references.iterator()
    }
}