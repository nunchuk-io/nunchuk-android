package com.nunchuk.android.auth.components.verify

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface VerifyNewDeviceModule {

    @Binds
    @IntoMap
    @ViewModelKey(VerifyNewDeviceViewModel::class)
    fun bindVerifyNewDeviceViewModel(verifyNewDeviceViewModel: VerifyNewDeviceViewModel): ViewModel

}