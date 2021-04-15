package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.add.AddWalletModule
import com.nunchuk.android.wallet.assign.AssignSignerActivity
import com.nunchuk.android.wallet.assign.AssignSignerModule
import com.nunchuk.android.wallet.backup.BackupWalletActivity
import com.nunchuk.android.wallet.backup.BackupWalletModule
import com.nunchuk.android.wallet.confirm.WalletConfirmActivity
import com.nunchuk.android.wallet.confirm.WalletConfirmModule
import com.nunchuk.android.wallet.upload.UploadConfigurationActivity
import com.nunchuk.android.wallet.upload.UploadConfigurationModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface WalletActivityModule {

    @ContributesAndroidInjector(modules = [AddWalletModule::class])
    fun addWalletActivity(): AddWalletActivity

    @ContributesAndroidInjector(modules = [AssignSignerModule::class])
    fun assignSignerActivity(): AssignSignerActivity

    @ContributesAndroidInjector(modules = [WalletConfirmModule::class])
    fun walletConfirmActivity(): WalletConfirmActivity

    @ContributesAndroidInjector(modules = [BackupWalletModule::class])
    fun backupWalletActivity(): BackupWalletActivity

    @ContributesAndroidInjector(modules = [UploadConfigurationModule::class])
    fun uploadConfigurationActivity(): UploadConfigurationActivity
}