package com.nunchuk.android.settings.di

import com.nunchuk.android.settings.AccountFragment
import com.nunchuk.android.settings.AccountSettingActivity
import com.nunchuk.android.settings.DeleteAccountActivity
import com.nunchuk.android.settings.network.NetworkSettingActivity
import com.nunchuk.android.settings.unit.DisplayUnitActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface SettingsViewModule {

    @ContributesAndroidInjector
    fun accountFragment(): AccountFragment

    @ContributesAndroidInjector
    fun networkSettingFragment(): NetworkSettingActivity

    @ContributesAndroidInjector
    fun displayUnitActivity(): DisplayUnitActivity

    @ContributesAndroidInjector
    fun accountSettingActivity(): AccountSettingActivity

    @ContributesAndroidInjector
    fun deleteAccountActivity(): DeleteAccountActivity

}