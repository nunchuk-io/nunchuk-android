package com.nunchuk.android.signer.software.di

import dagger.Module

@Module(
    includes = [
        SoftwareSignerActivityModule::class,
        SoftwareSignerViewModelModule::class
    ]
)
interface SoftwareSignerModule
