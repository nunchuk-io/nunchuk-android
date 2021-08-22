package com.nunchuk.android.signer.software.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedViewModel
import com.nunchuk.android.signer.software.components.create.CreateNewSeedViewModel
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameViewModel
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseViewModel
import com.nunchuk.android.signer.software.components.recover.RecoverSeedViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface SoftwareSignerViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CreateNewSeedViewModel::class)
    fun bindCreateNewSeedViewModel(viewModel: CreateNewSeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RecoverSeedViewModel::class)
    fun bindRecoverSeedViewModel(viewModel: RecoverSeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmSeedViewModel::class)
    fun bindConfirmSeedViewModel(viewModel: ConfirmSeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSoftwareSignerNameViewModel::class)
    fun bindAddSoftwareSignerNameViewModel(viewModel: AddSoftwareSignerNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetPassphraseViewModel::class)
    fun bindSetPassphraseViewModel(viewModel: SetPassphraseViewModel): ViewModel

}