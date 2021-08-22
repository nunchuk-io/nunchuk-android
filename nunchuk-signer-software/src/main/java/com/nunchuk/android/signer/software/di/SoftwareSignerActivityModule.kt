package com.nunchuk.android.signer.software.di

import com.nunchuk.android.signer.software.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedActivity
import com.nunchuk.android.signer.software.components.create.CreateNewSeedActivity
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameActivity
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseActivity
import com.nunchuk.android.signer.software.components.recover.RecoverSeedActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface SoftwareSignerActivityModule {

    @ContributesAndroidInjector
    fun softwareSignerIntroActivity(): SoftwareSignerIntroActivity

    @ContributesAndroidInjector
    fun createNewSeedActivity(): CreateNewSeedActivity

    @ContributesAndroidInjector
    fun recoverSeedActivity(): RecoverSeedActivity

    @ContributesAndroidInjector
    fun confirmSeedActivity(): ConfirmSeedActivity

    @ContributesAndroidInjector
    fun addSoftwareSignerNameActivity(): AddSoftwareSignerNameActivity

    @ContributesAndroidInjector
    fun setPassphraseActivity(): SetPassphraseActivity

}
