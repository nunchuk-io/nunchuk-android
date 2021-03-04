package com.nunchuk.android.app.di

import com.nunchuk.android.app.splash.SplashActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AppActivityModule {

    @ContributesAndroidInjector
    abstract fun splashActivity(): SplashActivity
}
