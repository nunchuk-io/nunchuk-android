package com.nunchuk.android.core.di

import android.content.Context
import com.nunchuk.android.core.matrix.SessionHolder
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@EntryPoint
interface SingletonEntryPoint {
    fun sessionHolder(): SessionHolder
}

fun Context.singletonEntryPoint(): SingletonEntryPoint {
    return EntryPoints.get(applicationContext, SingletonEntryPoint::class.java)
}