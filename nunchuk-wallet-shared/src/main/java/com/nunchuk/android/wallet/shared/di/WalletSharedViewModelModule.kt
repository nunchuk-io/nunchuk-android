package com.nunchuk.android.wallet.shared.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerViewModel
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigViewModel
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletViewModel
import com.nunchuk.android.wallet.shared.components.create.CreateSharedWalletViewModel
import com.nunchuk.android.wallet.shared.components.review.ReviewSharedWalletViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletSharedViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CreateSharedWalletViewModel::class)
    fun bindCreateSharedWalletViewModel(viewModel: CreateSharedWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfigureSharedWalletViewModel::class)
    fun bindConfigureSharedWalletViewModel(viewModel: ConfigureSharedWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssignSignerViewModel::class)
    fun bindAssignSignerViewModel(viewModel: AssignSignerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReviewSharedWalletViewModel::class)
    fun bindReviewSharedWalletViewModel(viewModel: ReviewSharedWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedWalletConfigViewModel::class)
    fun bindSharedWalletConfigViewModel(viewModel: SharedWalletConfigViewModel): ViewModel

}