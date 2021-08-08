package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ReviewWalletModule {

    @Binds
    @IntoMap
    @ViewModelKey(ReviewWalletViewModel::class)
    fun bindReviewWalletViewModel(viewModel: ReviewWalletViewModel): ViewModel

}