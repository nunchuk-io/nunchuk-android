package com.nunchuk.android.transaction.export

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ExportTransactionModule {

    @Binds
    @IntoMap
    @ViewModelKey(ExportTransactionViewModel::class)
    fun bindExportTransactionViewModel(viewModelExport: ExportTransactionViewModel): ViewModel

}