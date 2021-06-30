package com.nunchuk.android.database.di

import dagger.Module

@Module(
    includes = [
        NunchukPersistenceModule::class
    ]
)
interface DatabaseProxyModule
