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
import com.nunchuk.android.main.membership.byzantine.key.toRecurringPaymentType
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.payment.PaymentCalculationMethod
import com.nunchuk.android.model.payment.PaymentDestinationType
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.premier.CreateRecurringPaymentUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val createRecurringPaymentUseCase: CreateRecurringPaymentUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>(RecurringPaymentActivity.GROUP_ID).orEmpty()
    private val walletId =
        savedStateHandle.get<String>(RecurringPaymentActivity.WALLET_ID).orEmpty()
    private val _config = MutableSaveStateFlow(
        savedStateHandle = savedStateHandle,
        key = "recurring_payment_config",
        defaultValue = RecurringPaymentConfig()
    )
    val config = _config.asStateFlow()

    private val _state = MutableStateFlow(RecurringPaymentUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _state.update {
                    it.copy(
                        hasServerKey = wallet.signers.any { signer -> signer.type == SignerType.SERVER },
                    )
                 }
            }
        }
    }

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

    fun onSubmit() {
        viewModelScope.launch {
            val recurringPayment = RecurringPayment(
                name = config.value.name,
                paymentType = if (config.value.useAmount) RecurringPaymentType.FIXED_AMOUNT else RecurringPaymentType.PERCENTAGE,
                destinationType = if (config.value.addresses.isNotEmpty()) PaymentDestinationType.WHITELISTED_ADDRESSES else PaymentDestinationType.DESTINATION_WALLET,
                frequency = config.value.frequency!!,
                startDate = config.value.startDate,
                endDate = config.value.endDate,
                allowCosigning = config.value.isCosign!!,
                note = config.value.note,
                amount = config.value.amount.toDoubleOrNull() ?: 0.0,
                currency = config.value.unit.toRecurringPaymentType(),
                calculationMethod = config.value.calculatePercentageJustInTime?.let {
                    if (it) PaymentCalculationMethod.JUST_IN_TIME else PaymentCalculationMethod.RUNNING_AVERAGE
                },
                bsms = config.value.bsms,
                addresses = config.value.addresses,
                feeRate = config.value.feeRate,
            )
            createRecurringPaymentUseCase(
                CreateRecurringPaymentUseCase.Params(
                    groupId = groupId,
                    walletId = walletId,
                    recurringPayment = recurringPayment,
                )
            ).onSuccess { payload ->
                _state.update {
                    it.copy(
                        openDummyTransactionScreen = payload,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        errorMessage = error.message.orEmpty(),
                    )
                }
            }
        }
    }

    fun onOpenDummyTransactionScreenComplete() {
        _state.update {
            it.copy(
                openDummyTransactionScreen = null,
            )
        }
    }

    fun onErrorMessageShown() {
        _state.update {
            it.copy(
                errorMessage = null,
            )
        }
    }
}