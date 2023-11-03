package com.nunchuk.android.main.membership.byzantine.payment

import androidx.lifecycle.ViewModel
import com.nunchuk.android.model.SpendingCurrencyUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class RecurringPaymentViewModel @Inject constructor(

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
}