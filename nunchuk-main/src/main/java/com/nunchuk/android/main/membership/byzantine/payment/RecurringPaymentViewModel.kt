package com.nunchuk.android.main.membership.byzantine.payment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.base.MutableSaveStateFlow
import com.nunchuk.android.core.base.update
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.wallet.GetAddressWalletUseCase
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.key.toRecurringPaymentType
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.payment.PaymentCalculationMethod
import com.nunchuk.android.model.payment.PaymentDestinationType
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.premier.CreateRecurringPaymentUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.usecase.wallet.GetWallets2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentViewModel @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val createRecurringPaymentUseCase: CreateRecurringPaymentUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
    private val getAddressWalletUseCase: GetAddressWalletUseCase,
    private val getWallets2UseCase: GetWallets2UseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val groupId = savedStateHandle.get<String>(RecurringPaymentActivity.GROUP_ID).orEmpty()
    val walletId =
        savedStateHandle.get<String>(RecurringPaymentActivity.WALLET_ID).orEmpty()
    val myRole = savedStateHandle.get<AssistedWalletRole>(RecurringPaymentActivity.ROLE) ?: AssistedWalletRole.NONE
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
                val hasServerKey = wallet.signers.any { signer -> signer.type == SignerType.SERVER }
                _state.update {
                    it.copy(
                        hasServerKey = hasServerKey,
                    )
                }
                _config.update {
                    it.copy(
                        isCosign = hasServerKey,
                    )
                }
            }
        }
        getWallets()
    }

    fun init() {
        _config.value = RecurringPaymentConfig(isCosign = _state.value.hasServerKey)
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
            it.copy(unit = unit, amount = "")
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
                val content = it.readText()
                parseWalletDescriptorUseCase(content).onSuccess { wallet ->
                    getBsms(wallet)
                }.onFailure {
                    _state.update { state ->
                        state.copy(
                            errorMessage = application.getString(R.string.nc_invalid_wallet_configuration),
                        )
                    }
                }
            }
        }
    }

    fun getBsms(wallet: Wallet) {
        if (wallet.id == walletId) {
            _state.update { state ->
                state.copy(
                    isMyWallet = true,
                )
            }
            return
        }
        viewModelScope.launch {
            getWalletBsmsUseCase(wallet).onSuccess { bsms ->
                _config.update {
                    it.copy(bsms = bsms)
                }
                _state.update {
                    it.copy(
                        openBsmsScreen = bsms,
                    )
                }
            }.onFailure { e ->
                _state.update { state ->
                    state.copy(
                        errorMessage = e.message.orEmpty(),
                    )
                }
            }
            getAddressWalletUseCase(
                GetAddressWalletUseCase.Params(
                    wallet,
                    0,
                    0
                )
            ).onSuccess { addresses ->
                _config.update {
                    it.copy(addresses = addresses)
                }
            }.onFailure { e ->
                _state.update { state ->
                    state.copy(
                        errorMessage = e.message.orEmpty(),
                    )
                }
            }
        }
    }

    fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                getBsms(wallet)
            }.onFailure { e ->
                _state.update { state ->
                    state.copy(errorMessage = e.message.orEmpty())
                }
            }
        }
    }

    private fun getWallets() {
        viewModelScope.launch {
            getWallets2UseCase(Unit).onSuccess { wallets ->
                _state.update { it.copy(otherwallets = wallets.filterNot { wallet -> wallet.id == walletId }) }
            }.onFailure { e ->
                _state.update { state ->
                    state.copy(errorMessage = e.message.orEmpty())
                }
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
        Timber.d("onStartDateChange: $startDate")
        _config.update {
            it.copy(startDate = startDate)
        }
    }

    fun onEndDateChange(endDate: Long) {
        Timber.d("onEndDateChange: $endDate")
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
                bsms = null,
            )
        }
    }

    fun onSubmit() {
        viewModelScope.launch {
            val recurringPayment = RecurringPayment(
                name = config.value.name,
                paymentType = if (config.value.useAmount) RecurringPaymentType.FIXED_AMOUNT else RecurringPaymentType.PERCENTAGE,
                destinationType = if (config.value.bsms.isNullOrEmpty()) PaymentDestinationType.WHITELISTED_ADDRESSES else PaymentDestinationType.DESTINATION_WALLET,
                frequency = config.value.frequency,
                startDate = config.value.startDate,
                endDate = config.value.endDate.takeIf { !config.value.noEndDate } ?: 0L,
                allowCosigning = config.value.isCosign,
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

    fun onIsMyWallet() {
        _state.update {
            it.copy(
                isMyWallet = false,
            )
        }
    }

    fun onOpenBsmsScreenComplete() {
        _state.update {
            it.copy(
                openBsmsScreen = null,
            )
        }
    }
}