package com.nunchuk.android.signer.di

import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.add.AddSignerActivity
import com.nunchuk.android.signer.add.AddSignerModule
import com.nunchuk.android.signer.details.SignerInfoActivity
import com.nunchuk.android.signer.details.SignerInfoModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface SignerActivityModule {

    @ContributesAndroidInjector
    fun signerIntroActivity(): SignerIntroActivity

    @ContributesAndroidInjector(modules = [SignerInfoModule::class])
    fun signerInfoActivity(): SignerInfoActivity

    @ContributesAndroidInjector(modules = [AddSignerModule::class])
    fun addSignerActivity(): AddSignerActivity
}
