package com.nunchuk.android.transaction.send.fee

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface EstimatedFeeModule {

    @Binds
    @IntoMap
    @ViewModelKey(EstimatedFeeViewModel::class)
    fun bindEstimatedFeeViewModel(viewModel: EstimatedFeeViewModel): ViewModel

}