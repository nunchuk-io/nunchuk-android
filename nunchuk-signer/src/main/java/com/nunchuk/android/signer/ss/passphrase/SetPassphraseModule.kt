package com.nunchuk.android.signer.ss.passphrase

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface SetPassphraseModule {

    @Binds
    @IntoMap
    @ViewModelKey(SetPassphraseViewModel::class)
    fun bindSetPassphraseViewModel(viewModel: SetPassphraseViewModel): ViewModel

}