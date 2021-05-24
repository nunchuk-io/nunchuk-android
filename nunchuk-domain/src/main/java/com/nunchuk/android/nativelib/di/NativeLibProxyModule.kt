package com.nunchuk.android.nativelib.di

import dagger.Module

@Module(includes = [
    NativeLibDomainModule::class
])
interface NativeLibProxyModule
