package com.nunchuk.android.messages.di

import dagger.Module

@Module(
    includes = [
        MessagesActivityModule::class,
        ContactsUseCaseModule::class,
        MessagesUseCaseModule::class,
        MessagesDataModule::class,
        MessagesNetworkModule::class,
        MessagesCommonModule::class
    ]
)
interface MessagesProxyModule
