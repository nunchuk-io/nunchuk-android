package com.nunchuk.android.app.di

import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.splash.SplashModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AppActivityModule {

    @ContributesAndroidInjector(modules = [SplashModule::class])
    abstract fun splashActivity(): SplashActivity

    @ContributesAndroidInjector
    abstract fun introActivity(): IntroActivity

}
