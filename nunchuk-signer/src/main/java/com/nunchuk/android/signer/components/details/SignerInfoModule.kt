package com.nunchuk.android.signer.components.details

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface SignerInfoModule {

    @Binds
    @IntoMap
    @ViewModelKey(SignerInfoViewModel::class)
    fun bindSignerInfoViewModel(viewModel: SignerInfoViewModel): ViewModel

}