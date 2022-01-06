package com.nunchuk.android.wallet.personal.di

import com.nunchuk.android.wallet.personal.components.add.AddWalletActivity
import com.nunchuk.android.wallet.personal.components.recover.AddRecoverWalletActivity
import com.nunchuk.android.wallet.personal.components.WalletIntermediaryActivity
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletQrCodeActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface WalletPersonalActivityModule {

    @ContributesAndroidInjector
    fun addWalletActivity(): AddWalletActivity

    @ContributesAndroidInjector(modules = [WalletIntermediaryViewModelModule::class])
    fun walletIntermediaryActivity(): WalletIntermediaryActivity

    @ContributesAndroidInjector(modules = [AddRecoverWalletViewModelModule::class])
    fun addRecoverWalletInfoActivity(): AddRecoverWalletActivity

    @ContributesAndroidInjector(modules = [RecoverWalletQRCodeViewModelModule::class])
    fun recoverWalletQrCodeActivity(): RecoverWalletQrCodeActivity

}