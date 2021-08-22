package com.nunchuk.android.signer.di

import com.nunchuk.android.signer.AirSignerIntroActivity
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.components.add.AddSignerActivity
import com.nunchuk.android.signer.components.add.AddSignerModule
import com.nunchuk.android.signer.components.details.SignerInfoActivity
import com.nunchuk.android.signer.components.details.SignerInfoModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface SignerActivityModule {

    @ContributesAndroidInjector
    fun signerIntroActivity(): SignerIntroActivity

    @ContributesAndroidInjector
    fun airSignerIntroActivity(): AirSignerIntroActivity

    @ContributesAndroidInjector(modules = [SignerInfoModule::class])
    fun signerInfoActivity(): SignerInfoActivity

    @ContributesAndroidInjector(modules = [AddSignerModule::class])
    fun addSignerActivity(): AddSignerActivity

}
