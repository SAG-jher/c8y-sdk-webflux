package io.c8y.scripts

import io.c8y.config.Platform

fun main() {

    val platform = Platform["schindler-prod"]
    println(platform.rest().inventory().count("query" to  "\$filter%3D(has(%27@schindler_ioee_ncm_core_NCMRecord%27)%20and%20@schindler_ioee_ncm_core_NCMRecord.version%20eq%2010)")
        .block())
}