package com.nunchuk.android.core.mapper

fun <F, T> Mapper<F, T>.toListMapper(): (List<F>?) -> List<T> {
    return { list ->
        list?.map { item -> map(item) } ?: listOf()
    }
}

fun <F, T> IndexedMapper<F, T>.toListMapper(): (List<F>) -> List<T> {
    return { list -> list.mapIndexed { index, item -> map(index, item) } }
}
