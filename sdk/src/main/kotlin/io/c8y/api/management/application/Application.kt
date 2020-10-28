package io.c8y.api.management.application


import io.c8y.api.management.tenant.TenantReference
import io.c8y.api.support.Dynamic

data class Application(
    val id: String? = null,
    val name: String? = null,
    val activeVersionId: String? = null,
    val owner: TenantReference? = null,
    val type: ApplicationType? = null,
    val key: String? = null
) : Dynamic<Application>()