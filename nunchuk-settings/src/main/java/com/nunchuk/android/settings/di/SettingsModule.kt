package com.nunchuk.android.settings.di

import dagger.Module

@Module(
    includes = [
        SettingsViewModule::class,
        SettingsViewModelModule::class,
    ]
)
interface SettingsModule
