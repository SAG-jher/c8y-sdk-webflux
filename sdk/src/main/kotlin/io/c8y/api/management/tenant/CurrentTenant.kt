package io.c8y.api.management.tenant

import io.c8y.api.support.Dynamic

data class CurrentTenant(val name: String?,val domainName:String?) : Dynamic<CurrentTenant>() {

}
