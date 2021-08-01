package com.nunchuk.android.transaction.di

import com.nunchuk.android.transaction.components.details.TransactionDetailsActivity
import com.nunchuk.android.transaction.components.details.TransactionDetailsModule
import com.nunchuk.android.transaction.components.export.ExportTransactionActivity
import com.nunchuk.android.transaction.components.export.ExportTransactionModule
import com.nunchuk.android.transaction.components.imports.ImportTransactionActivity
import com.nunchuk.android.transaction.components.imports.ImportTransactionModule
import com.nunchuk.android.transaction.components.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.components.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.components.receive.di.AddressFragmentModule
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.amount.InputAmountModule
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmModule
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeActivity
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeModule
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptActivity
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface TransactionActivityModule {

    @ContributesAndroidInjector(modules = [AddressFragmentModule::class])
    fun receiveTransactionActivity(): ReceiveTransactionActivity

    @ContributesAndroidInjector
    fun addressDetailsActivity(): AddressDetailsActivity

    @ContributesAndroidInjector(modules = [InputAmountModule::class])
    fun inputAmountActivity(): InputAmountActivity

    @ContributesAndroidInjector(modules = [EstimatedFeeModule::class])
    fun estimatedFeeActivity(): EstimatedFeeActivity

    @ContributesAndroidInjector(modules = [AddReceiptModule::class])
    fun addReceiptActivity(): AddReceiptActivity

    @ContributesAndroidInjector(modules = [TransactionConfirmModule::class])
    fun transactionConfirmActivity(): TransactionConfirmActivity

    @ContributesAndroidInjector(modules = [TransactionDetailsModule::class])
    fun transactionDetailActivity(): TransactionDetailsActivity

    @ContributesAndroidInjector(modules = [ExportTransactionModule::class])
    fun exportTransactionActivity(): ExportTransactionActivity

    @ContributesAndroidInjector(modules = [ImportTransactionModule::class])
    fun importTransactionActivity(): ImportTransactionActivity
}