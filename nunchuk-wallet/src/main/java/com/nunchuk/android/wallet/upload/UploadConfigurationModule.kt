package com.nunchuk.android.wallet.upload

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface UploadConfigurationModule {

    @Binds
    @IntoMap
    @ViewModelKey(UploadConfigurationViewModel::class)
    fun bindUploadConfigurationViewModel(viewModel: UploadConfigurationViewModel): ViewModel

}