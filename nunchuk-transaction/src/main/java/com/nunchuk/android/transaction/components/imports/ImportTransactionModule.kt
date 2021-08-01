package com.nunchuk.android.transaction.components.imports

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ImportTransactionModule {

    @Binds
    @IntoMap
    @ViewModelKey(ImportTransactionViewModel::class)
    fun bindImportTransactionViewModel(viewModel: ImportTransactionViewModel): ViewModel

}