package io.c8y.api.management.application


data class ApplicationSubscriptions(
    val users:List<ApplicationUser>
)

data class ApplicationUser(
    val tenant : String,
    val name: String,
    val password: String
)