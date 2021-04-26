package com.nunchuk.android.signer.ss.recover

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface RecoverSeedModule {

    @Binds
    @IntoMap
    @ViewModelKey(RecoverSeedViewModel::class)
    fun bindRecoverSeedModule(viewModel: RecoverSeedViewModel): ViewModel

}
