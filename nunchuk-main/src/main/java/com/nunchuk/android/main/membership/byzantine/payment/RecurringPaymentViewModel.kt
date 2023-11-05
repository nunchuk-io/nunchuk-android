package com.nunchuk.android.main.membership.byzantine.payment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SpendingCurrencyUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentViewModel @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _config = MutableStateFlow(RecurringPaymentConfig())
    val config = _config.asStateFlow()

    fun onNameChange(name: String) {
        _config.update {
            it.copy(name = name)
        }
    }

    fun onAmountChange(amount: String) {
        _config.update {
            it.copy(amount = amount)
        }
    }

    fun onUnitChange(unit: SpendingCurrencyUnit) {
        _config.update {
            it.copy(unit = unit)
        }
    }

    fun onUseAmountChange(useAmount: Boolean) {
        _config.update {
            it.copy(useAmount = useAmount)
        }
    }

    fun openBsms(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            getFileFromUri(application.contentResolver, uri,  application.cacheDir)?.let {

            }
        }
    }

    fun onCalculatePercentageJustInTimeChange(value: Boolean) {
        _config.update {
            it.copy(calculatePercentageJustInTime = value)
        }
    }
}