package com.nunchuk.android.main.membership.key.server.limit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigSpendingLimitViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = ConfigSpendingLimitFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ConfigSpendingLimitEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfigSpendingLimitState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        _state.update {
            it.copy(
                timeUnit = args.keyPolicy?.spendingPolicy?.timeUnit ?: SpendingTimeUnit.DAILY,
                currencyUnit = args.keyPolicy?.spendingPolicy?.currencyUnit ?: SpendingCurrencyUnit.USD
            )
        }
    }

    fun showTimeUnitOption() {
        viewModelScope.launch {
            _event.emit(ConfigSpendingLimitEvent.ShowTimeUnit)
        }
    }

    fun showCurrencyUnitOption() {
        viewModelScope.launch {
            _event.emit(ConfigSpendingLimitEvent.ShowCurrencyUnit)
        }
    }

    fun onContinueClicked(value: Long) {
        viewModelScope.launch {
            _event.emit(
                ConfigSpendingLimitEvent.ContinueClicked(
                    SpendingPolicy(
                        limit = value,
                        timeUnit = _state.value.timeUnit,
                        currencyUnit = _state.value.currencyUnit,
                    )
                )
            )
        }
    }

    fun setTimeUnit(unit: SpendingTimeUnit) {
        _state.update { it.copy(timeUnit = unit) }
    }

    fun setCurrencyUnit(unit: SpendingCurrencyUnit) {
        _state.update { it.copy(currencyUnit = unit) }
    }
}

data class ConfigSpendingLimitState(
    val currencyUnit: SpendingCurrencyUnit = SpendingCurrencyUnit.USD,
    val timeUnit: SpendingTimeUnit = SpendingTimeUnit.DAILY
)

sealed class ConfigSpendingLimitEvent {
    object ShowTimeUnit : ConfigSpendingLimitEvent()
    object ShowCurrencyUnit : ConfigSpendingLimitEvent()
    data class ContinueClicked(val spendingPolicy: SpendingPolicy) : ConfigSpendingLimitEvent()
}

fun SpendingCurrencyUnit.toLabel(context: Context) = when (this) {
    SpendingCurrencyUnit.USD -> context.getString(R.string.nc_currency_usd)
    SpendingCurrencyUnit.BTC -> context.getString(R.string.nc_currency_btc)
    SpendingCurrencyUnit.sat -> context.getString(R.string.nc_currency_sat)
}

fun SpendingTimeUnit.toLabel(context: Context) = when (this) {
    SpendingTimeUnit.DAILY -> context.getString(R.string.nc_daily)
    SpendingTimeUnit.MONTHLY -> context.getString(R.string.nc_monthly)
    SpendingTimeUnit.WEEKLY -> context.getString(R.string.nc_weekly)
    SpendingTimeUnit.YEARLY -> context.getString(R.string.nc_yearly)
}