package io.c8y.api.support

import org.springframework.http.MediaType


object PlatformMediaType {

    const val VND_COM_NSN_CUMULOCITY = "vnd.com.nsn.cumulocity."

    const val APPLICATION_VND_COM_NSN_CUMULOCITY = "application/$VND_COM_NSN_CUMULOCITY"

    const val VND_COM_NSN_CUMULOCITY_CHARSET = "charset=utf-8"

    const val VND_COM_NSN_CUMULOCITY_VERSION = "ver=0.9"

    const val VND_COM_NSN_CUMULOCITY_PARAMS =
        "$VND_COM_NSN_CUMULOCITY_CHARSET;$VND_COM_NSN_CUMULOCITY_VERSION"
    const val MANAGED_OBJECT_TYPE =
        APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObject+json;" + VND_COM_NSN_CUMULOCITY_PARAMS

    const val MANAGED_OBJECT_COLLECTION_TYPE =
        APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS

    const val MANAGED_OBJECT_REFERENCE_TYPE =
        APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS

    const val MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE_VALUE =
        APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS


    val MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE =
        MediaType.parseMediaType(MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE_VALUE)
}