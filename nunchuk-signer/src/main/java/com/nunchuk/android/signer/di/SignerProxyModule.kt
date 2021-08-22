package com.nunchuk.android.signer.di

import com.nunchuk.android.signer.software.di.SoftwareSignerModule
import dagger.Module

@Module(
    includes = [
        SignerActivityModule::class,
        SoftwareSignerModule::class
    ]
)
interface SignerProxyModule
