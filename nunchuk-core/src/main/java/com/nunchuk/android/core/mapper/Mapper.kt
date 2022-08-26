package com.nunchuk.android.core.mapper

interface Mapper<F, T> {
    fun map(from: F): T
}

interface IndexedMapper<F, T> {
     fun map(index: Int, from: F): T
}