package com.nunchuk.android.main.di

import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.main.components.signer.AddSignerActivity
import com.nunchuk.android.main.components.signer.AddSignerModule
import com.nunchuk.android.main.components.signer.SignerInfoActivity
import com.nunchuk.android.main.components.signer.SignerIntroActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MainActivityModule {

    @ContributesAndroidInjector(modules = [MainFragmentModule::class])
    fun mainActivity(): MainActivity

    @ContributesAndroidInjector
    fun signerIntroActivity(): SignerIntroActivity

    @ContributesAndroidInjector
    fun signerInfoActivity(): SignerInfoActivity

    @ContributesAndroidInjector(modules = [AddSignerModule::class])
    fun addSignerActivity(): AddSignerActivity
}
