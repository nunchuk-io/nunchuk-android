package com.nunchuk.android.transaction.di

import dagger.Module

@Module(
    includes = [
        TransactionActivityModule::class
    ]
)
interface TransactionProxyModule