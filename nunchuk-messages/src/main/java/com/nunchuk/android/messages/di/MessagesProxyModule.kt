package com.nunchuk.android.messages.di

import dagger.Module

@Module(
    includes = [
        MessagesActivityModule::class,
        MessagesUseCaseModule::class,
        MessagesViewModelModule::class
    ]
)
interface MessagesProxyModule
