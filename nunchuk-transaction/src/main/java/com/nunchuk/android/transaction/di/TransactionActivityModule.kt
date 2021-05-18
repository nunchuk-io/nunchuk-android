package com.nunchuk.android.transaction.di

import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.receive.ReceiveTransactionModule
import com.nunchuk.android.transaction.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.receive.di.AddressFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface TransactionActivityModule {

    @ContributesAndroidInjector(modules = [ReceiveTransactionModule::class, AddressFragmentModule::class])
    fun receiveTransactionActivity(): ReceiveTransactionActivity

    @ContributesAndroidInjector
    fun addressDetailsActivity(): AddressDetailsActivity

}