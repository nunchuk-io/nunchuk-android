package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.components.add.AddWalletActivity
import com.nunchuk.android.wallet.components.add.AddWalletModule
import com.nunchuk.android.wallet.components.configure.ConfigureWalletActivity
import com.nunchuk.android.wallet.components.configure.ConfigureWalletModule
import com.nunchuk.android.wallet.components.backup.BackupWalletActivity
import com.nunchuk.android.wallet.components.backup.BackupWalletModule
import com.nunchuk.android.wallet.components.config.WalletConfigActivity
import com.nunchuk.android.wallet.components.config.WalletConfigModule
import com.nunchuk.android.wallet.components.confirm.WalletConfirmActivity
import com.nunchuk.android.wallet.components.confirm.WalletConfirmModule
import com.nunchuk.android.wallet.components.details.WalletDetailsActivity
import com.nunchuk.android.wallet.components.details.WalletDetailsModule
import com.nunchuk.android.wallet.components.intro.WalletEmptySignerActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface WalletActivityModule {

    @ContributesAndroidInjector
    fun walletEmptySignerActivity(): WalletEmptySignerActivity

    @ContributesAndroidInjector(modules = [AddWalletModule::class])
    fun addWalletActivity(): AddWalletActivity

    @ContributesAndroidInjector(modules = [ConfigureWalletModule::class])
    fun configureWalletActivity(): ConfigureWalletActivity

    @ContributesAndroidInjector(modules = [WalletConfirmModule::class])
    fun walletConfirmActivity(): WalletConfirmActivity

    @ContributesAndroidInjector(modules = [BackupWalletModule::class])
    fun backupWalletActivity(): BackupWalletActivity

    @ContributesAndroidInjector(modules = [UploadConfigurationModule::class])
    fun uploadConfigurationActivity(): UploadConfigurationActivity

    @ContributesAndroidInjector(modules = [WalletConfigModule::class])
    fun walletInfoActivity(): WalletConfigActivity

    @ContributesAndroidInjector(modules = [WalletDetailsModule::class])
    fun walletDetailsActivity(): WalletDetailsActivity

}