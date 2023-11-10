package com.nunchuk.android.main.membership.byzantine.payment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.base.MutableSaveStateFlow
import com.nunchuk.android.core.base.update
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.SpendingCurrencyUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentViewModel @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _config = MutableSaveStateFlow(
        savedStateHandle = savedStateHandle,
        key = "recurring_payment_config",
        defaultValue = RecurringPaymentConfig()
    )
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
            getFileFromUri(application.contentResolver, uri, application.cacheDir)?.let {

            }
        }
    }

    fun onAddressesChange(addresses: List<String>) {
        _config.update {
            it.copy(addresses = addresses)
        }
    }

    fun onCalculatePercentageJustInTimeChange(value: Boolean) {
        _config.update {
            it.copy(calculatePercentageJustInTime = value)
        }
    }

    fun onFrequencyChange(frequency: PaymentFrequency) {
        _config.update {
            it.copy(frequency = frequency)
        }
    }

    fun onStartDateChange(startDate: Long) {
        _config.update {
            it.copy(startDate = startDate)
        }
    }

    fun onEndDateChange(endDate: Long) {
        _config.update {
            it.copy(endDate = endDate)
        }
    }

    fun onNoteChange(note: String) {
        _config.update {
            it.copy(note = note)
        }
    }

    fun onNoEndDateChange(noEndDate: Boolean) {
        _config.update {
            it.copy(noEndDate = noEndDate)
        }
    }

    fun onIsCosignChange(isCosign: Boolean) {
        _config.update {
            it.copy(isCosign = isCosign)
        }
    }

    fun onFeeRateChange(feeRate: FeeRate) {
        _config.update {
            it.copy(feeRate = feeRate)
        }
    }

    fun clearAddressInfo() {
        _config.update {
            it.copy(
                addresses = emptyList(),
                note = "",
                isCosign = null,
            )
        }
    }
}