package com.nunchuk.android.contact.di

import dagger.Module

@Module(
    includes = [
        ContactViewModelModule::class,
        ContactFragmentModule::class,
        ContactNetworkModule::class,
        ContactDataModule::class,
        ContactUseCaseModule::class
    ]
)
interface ContactProxyModule
