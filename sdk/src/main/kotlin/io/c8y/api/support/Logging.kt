package io.c8y.api.support

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Object.loggerForThis(): Logger {
    return loggerFor(`class`)
}

inline fun <reified T> loggerFor(): Logger {
    return loggerFor(T::class.java)
}

fun loggerFor(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)


fun loggerFor(id: String): Logger = LoggerFactory.getLogger(id)

inline fun  Logger.info (crossinline body:()->String){
    if(this.isInfoEnabled){
        this.info(body())
    }
}