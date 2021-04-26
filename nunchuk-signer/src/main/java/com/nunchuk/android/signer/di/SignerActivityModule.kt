package com.nunchuk.android.signer.di

import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.add.AddSignerActivity
import com.nunchuk.android.signer.add.AddSignerModule
import com.nunchuk.android.signer.details.SignerInfoActivity
import com.nunchuk.android.signer.details.SignerInfoModule
import com.nunchuk.android.signer.ss.create.CreateNewSeedActivity
import com.nunchuk.android.signer.ss.create.CreateNewSeedModule
import com.nunchuk.android.signer.ss.recover.RecoverSeedActivity
import com.nunchuk.android.signer.ss.recover.RecoverSeedModule
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

    @ContributesAndroidInjector
    fun softwareSignerIntroActivity(): SoftwareSignerIntroActivity

    @ContributesAndroidInjector(modules = [CreateNewSeedModule::class])
    fun createNewSeedActivity(): CreateNewSeedActivity

    @ContributesAndroidInjector(modules = [RecoverSeedModule::class])
    fun recoverSeedActivity(): RecoverSeedActivity

}
