package com.nunchuk.android.transaction.di

import com.nunchuk.android.transaction.details.TransactionDetailsActivity
import com.nunchuk.android.transaction.details.TransactionDetailsModule
import com.nunchuk.android.transaction.export.ExportTransactionActivity
import com.nunchuk.android.transaction.export.ExportTransactionModule
import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.receive.ReceiveTransactionModule
import com.nunchuk.android.transaction.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.receive.di.AddressFragmentModule
import com.nunchuk.android.transaction.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.send.amount.InputAmountModule
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmModule
import com.nunchuk.android.transaction.send.fee.EstimatedFeeActivity
import com.nunchuk.android.transaction.send.fee.EstimatedFeeModule
import com.nunchuk.android.transaction.send.receipt.AddReceiptActivity
import com.nunchuk.android.transaction.send.receipt.AddReceiptModule
import com.nunchuk.android.transaction.imports.ImportTransactionActivity
import com.nunchuk.android.transaction.imports.ImportTransactionModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface TransactionActivityModule {

    @ContributesAndroidInjector(modules = [ReceiveTransactionModule::class, AddressFragmentModule::class])
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