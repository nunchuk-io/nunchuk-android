package com.nunchuk.android.messages.di

import dagger.Module

@Module(
    includes = [
        MessagesActivityModule::class,
        ContactsUseCaseModule::class,
        MessagesDataModule::class,
        MessagesNetworkModule::class,
        MessagesCommonModule::class
    ]
)
interface MessagesProxyModule
