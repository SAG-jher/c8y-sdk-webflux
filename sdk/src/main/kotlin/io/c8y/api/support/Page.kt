package io.c8y.api.support

data class Page(val next: String?, val pageSize: Long?, val totalPages: Long?) {
    fun hasNext(): Boolean {
        return next != null;
    }

    fun totalElements(): Long? {
        return if (pageSize != null && totalPages != null) {
            pageSize * totalPages;
        }else{
            null
        }
    }

    companion object {
        val empty = Page(null, null, null);
    }
}