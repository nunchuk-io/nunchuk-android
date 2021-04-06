package com.nunchuk.android.signer.di

import dagger.Module

@Module(
    includes = [
        SignerActivityModule::class
    ]
)
interface SignerProxyModule
