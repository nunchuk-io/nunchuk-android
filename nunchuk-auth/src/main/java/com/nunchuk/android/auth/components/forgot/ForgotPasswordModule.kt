package com.nunchuk.android.auth.components.forgot

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ForgotPasswordModule {

    @Binds
    @IntoMap
    @ViewModelKey(ForgotPasswordViewModel::class)
    fun bindForgotPasswordViewModel(viewModel: ForgotPasswordViewModel): ViewModel

}