package com.nunchuk.android.app.referral.invitefriend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.referral.ConfirmationCodeResultData
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.campaign.CreateReferrerCodeByEmailUseCase
import com.nunchuk.android.usecase.campaign.GetLocalReferrerCodeUseCase
import com.nunchuk.android.usecase.campaign.GetReferrerCodeByEmailUseCase
import com.nunchuk.android.usecase.campaign.SaveLocalReferrerCodeUseCase
import com.nunchuk.android.usecase.campaign.UpdateReceiveAddressByEmailUseCase
import com.nunchuk.android.usecase.coin.IsMyWalletUseCase
import com.nunchuk.android.usecase.network.IsNetworkConnectedUseCase
import com.nunchuk.android.usecase.wallet.GetMostRecentlyUsedWalletsUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralInviteFriendViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getReferrerCodeByEmailUseCase: GetReferrerCodeByEmailUseCase,
    private val createReferrerCodeByEmailUseCase: CreateReferrerCodeByEmailUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getMostRecentlyUsedWalletsUseCase: GetMostRecentlyUsedWalletsUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getLocalReferrerCodeUseCase: GetLocalReferrerCodeUseCase,
    private val saveLocalReferrerCodeUseCase: SaveLocalReferrerCodeUseCase,
    private val isMyWalletUseCase: IsMyWalletUseCase,
    private val updateReceiveAddressByEmailUseCase: UpdateReceiveAddressByEmailUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReferralInviteFriendUiState())
    val state = _state.asStateFlow()

    private var isInitialized = false
    private var currentData = ConfirmationCodeResultData.empty

    fun init(campaign: Campaign, localReferrerCode: ReferrerCode?) {
        if (isInitialized) return
        isInitialized = true
        _state.update {
            it.copy(campaign = campaign, localReferrerCode = localReferrerCode)
        }
        val accountInfo = accountManager.getAccount()
        val isLoginByEmail = accountInfo.loginType == SignInMode.EMAIL.value
        _state.update { it.copy(isLoginByEmail = isLoginByEmail) }
        if (localReferrerCode != null) {
            checkReferrerCodeByEmail(localReferrerCode.email)
            getWalletInfo(
                walletId = localReferrerCode.localWalletId,
                receiveAddress = localReferrerCode.receiveAddress
            )
        } else {
            if (isLoginByEmail) {
                getReferrerCodeByEmail(accountInfo.email)
            } else { // guest mode or primary key
                pickWallet()
            }
        }
        if (isLoginByEmail.not()) { // in case user change email and receive address is hidden
            getReceiveWalletAddressTemp()
        }
        viewModelScope.launch {
            getLocalReferrerCodeUseCase(Unit)
                .collect { result ->
                    if (result.getOrNull() != null) {
                        _state.update { it.copy(localReferrerCode = result.getOrThrow()) }
                    }
                }
        }
    }

    private fun getReceiveWalletAddressTemp() {
        viewModelScope.launch {
            val wallets = getMostRecentlyUsedWalletsUseCase(Unit).getOrNull().orEmpty()
            getAssistedWalletsFlowUseCase(Unit).collect { result ->
                val assistedWallets = result.getOrDefault(emptyList())
                val filterOutDeactivatedWallets = wallets.filter { wallet ->
                    assistedWallets.none { it.localId == wallet.id }
                            || assistedWallets.find { it.localId == wallet.id }?.status != WalletStatus.REPLACED.name
                }
                if (filterOutDeactivatedWallets.isNotEmpty()) {
                    val walletId = filterOutDeactivatedWallets.first().id
                    getUnusedWalletAddressUseCase(walletId).onSuccess { addresses ->
                        val address = addresses.first()
                        _state.update {
                            it.copy(
                                receiveWalletTemp = ReceiveWalletTemp(
                                    filterOutDeactivatedWallets.first(),
                                    address
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getWalletById(walletId: String) {
        if (walletId.isEmpty()) return
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId)
                .onSuccess { wallet ->
                    _state.update {
                        it.copy(wallet = wallet)
                    }
                }
        }
    }

    private fun pickWallet() = viewModelScope.launch {
        val wallets = getMostRecentlyUsedWalletsUseCase(Unit).getOrNull().orEmpty()
        getAssistedWalletsFlowUseCase(Unit).collect { result ->
            val assistedWallets = result.getOrDefault(emptyList())
            val filterOutDeactivatedWallets = wallets.filter { wallet ->
                assistedWallets.none { it.localId == wallet.id }
                        || assistedWallets.find { it.localId == wallet.id }?.status != WalletStatus.REPLACED.name
            }
            if (getWalletId().isEmpty() && filterOutDeactivatedWallets.isNotEmpty()) {
                val walletId = filterOutDeactivatedWallets.first().id
                savedStateHandle["walletId"] = walletId
                getWalletById(walletId)
                getFirstUnusedAddress()
            }
        }
    }

    private fun getFirstUnusedAddress() {
        if (getWalletId().isEmpty()) return
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(getWalletId()).onSuccess { addresses ->
                val address = addresses.first()
                _state.update { it.copy(pickReceiveAddress = address) }
                if (_state.value.isLoginByEmail) {
                    createReferrerCodeByEmail(
                        email = accountManager.getAccount().email,
                    )
                }
            }
        }
    }

    fun getReferrerCodeByEmail(email: String, resultData: ConfirmationCodeResultData? = null) {
        if (resultData != null && resultData == currentData) return
        currentData = resultData ?: ConfirmationCodeResultData.empty
        viewModelScope.launch {
            getReferrerCodeByEmailUseCase(
                GetReferrerCodeByEmailUseCase.Param(
                    email = email,
                    token = resultData?.token
                )
            ).onSuccess { referrerCode ->
                referrerCode?.let {
                    if (referrerCode.receiveAddress.isNotEmpty()) {
                        getWalletByAddress(referrerCode.receiveAddress)
                    }
                    val newReferrerCode = referrerCode.copy(email = email)
                    updateLocalReferrerCode(newReferrerCode)
                } ?: run {
                    pickWallet()
                }
            }.onFailure { error ->
                if (resultData?.token.isNullOrEmpty().not()) {
                    _state.update { it.copy(errorMsg = error.message) }
                }
            }
        }
    }

    private fun getWalletByAddress(address: String) {
        if (address.isEmpty()) return
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onException {}
                .collect { wallets ->
                    val myWallets = wallets.map { wallet ->
                        async {
                            try {
                                val isMyWallet = isMyWalletUseCase(
                                    IsMyWalletUseCase.Param(
                                        walletId = wallet.wallet.id,
                                        addresses = listOf(address)
                                    )
                                )
                                (isMyWallet.getOrDefault(false) to wallet.wallet)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll()
                    if (myWallets.isNotEmpty()) {
                        val wallet = myWallets.firstOrNull { it?.first == true }?.second
                        _state.update { it.copy(wallet = wallet) }
                    }
                }
        }
    }

    private fun checkReferrerCodeByEmail(email: String) { // in case user change address in other device
        if (email.isEmpty()) return
        viewModelScope.launch {
            getReferrerCodeByEmailUseCase(
                GetReferrerCodeByEmailUseCase.Param(
                    email,
                    null
                )
            ).onSuccess { referrerCode ->
                if (referrerCode != null) {
                    if (referrerCode.receiveAddress.isNotEmpty()) {
                        val newReferrerCode = referrerCode.copy(
                            email = email,
                            localWalletId = _state.value.localReferrerCode?.localWalletId.orEmpty()
                        )
                        getWalletInfo(
                            walletId = referrerCode.localWalletId,
                            receiveAddress = referrerCode.receiveAddress
                        )
                        updateLocalReferrerCode(newReferrerCode)
                    }
                    _state.update { it.copy(remoteReceiveAddressHash = referrerCode.receiveAddressHash) }
                }
            }
        }
    }

    fun createReferrerCodeByEmail(email: String) {
        if (isNetworkConnectedUseCase().not()) {
            _state.update { it.copy(showNoInternet = true) }
            return
        }
        val receiveAddress = if (isPickTempAddress()) {
            _state.value.receiveWalletTemp?.receiveAddress ?: return
        } else if (_state.value.pickReceiveAddress.isNullOrEmpty().not()) {
            _state.value.pickReceiveAddress!!
        } else {
            _state.value.localReferrerCode?.receiveAddress ?: return
        }
        viewModelScope.launch {
            createReferrerCodeByEmailUseCase(
                CreateReferrerCodeByEmailUseCase.Params(
                    email = email,
                    receiveAddress = receiveAddress,
                    walletId = if (isPickTempAddress()) {
                        _state.value.receiveWalletTemp?.wallet?.id.orEmpty()
                    } else {
                        getWalletId()
                    }
                )
            )
                .onSuccess { referrerCode ->
                    _state.update {
                        it.copy(
                            localReferrerCode = referrerCode,
                            forceShowInputEmail = false
                        )
                    }
                    if (isPickTempAddress()) getWalletById(_state.value.receiveWalletTemp?.wallet?.id.orEmpty()) else getWalletById(getWalletId())
                }
                .onFailure { error ->
                    if (error is NunchukApiException && error.code == 1409) {
                        _state.update { it.copy(showEmailAlreadyExist = true) }
                        return@onFailure
                    }
                    _state.update { it.copy(errorMsg = error.message) }
                }
        }
    }

    fun onErrorMessageEventConsumed() {
        _state.update { state -> state.copy(errorMsg = null) }
    }

    private fun getWalletId(): String {
        return savedStateHandle.get<String>("walletId") ?: ""
    }

    fun onShowEmailAlreadyExistDialogConsumed(isYesClick: Boolean, email: String) {
        if (isYesClick) {
            getReferrerCodeByEmail(email)
        }
        _state.update { it.copy(showEmailAlreadyExist = false) }
    }

    fun setForceShowInputEmail(show: Boolean) {
        _state.update { it.copy(forceShowInputEmail = show) }
    }

    fun getReceiveAddress(): String {
        return if (isPickTempAddress()) {
            _state.value.receiveWalletTemp?.receiveAddress.orEmpty()
        }
        else if (_state.value.isHideAddress()) {
            ""
        } else {
            _state.value.localReferrerCode?.receiveAddress ?: _state.value.pickReceiveAddress ?: ""
        }

    }

    fun isHasLocalReferrerCode(): Boolean {
        return _state.value.localReferrerCode != null
    }

    fun getSelectWalletId(): String {
        return if (isPickTempAddress()) {
            _state.value.receiveWalletTemp?.wallet?.id.orEmpty()
        } else if (_state.value.isHideAddress()) {
            ""
        } else {
            _state.value.localReferrerCode?.localWalletId.orEmpty().ifEmpty {
                _state.value.wallet?.id.orEmpty()
            }
        }
    }

    private fun updateLocalReferrerCode(referrerCode: ReferrerCode) {
        viewModelScope.launch {
            _state.update { it.copy(localReferrerCode = referrerCode) }
            saveLocalReferrerCodeUseCase(referrerCode)
        }
    }

    fun getEmail(): String {
        return _state.value.localReferrerCode?.email ?: ""
    }

    fun updatePickTempAddress(resultData: ConfirmationCodeResultData) {
        if (resultData == currentData) return
        currentData = resultData
        if (resultData.walletId.isNullOrEmpty().not()) {
            savedStateHandle["walletId"] = resultData.walletId
            getWalletInfo(resultData.walletId!!, resultData.address.orEmpty())
        }
        _state.update { it.copy(pickReceiveAddress = resultData.address.orEmpty()) }
    }

    fun updateReceiveAddress(resultData: ConfirmationCodeResultData) {
        if (isNetworkConnectedUseCase().not()) {
            _state.update { it.copy(showNoInternet = true) }
            return
        }
        if (currentData == resultData) return
        currentData = resultData
        viewModelScope.launch {
            updateReceiveAddressByEmailUseCase(
                UpdateReceiveAddressByEmailUseCase.Params(
                    email = getEmail(),
                    receiveAddress = resultData.address.orEmpty(),
                    token = resultData.token,
                    walletId = resultData.walletId.orEmpty()
                )
            ).onSuccess { result ->
                result?.let {
                    _state.update {
                        it.copy(
                            localReferrerCode = result,
                            forceShowInputEmail = false
                        )
                    }
                    getWalletInfo(resultData.walletId.orEmpty(), resultData.address.orEmpty())
                    _state.update { it.copy(event = ReferralInviteFriendEvent.ChangeAddressSuccess) }
                }
            }.onFailure { error ->
                _state.update { it.copy(errorMsg = error.message) }
            }
        }
    }

    fun isPickTempAddress(): Boolean {
        return _state.value.isHideAddress() && _state.value.forceShowInputEmail
    }

    fun onEventConsumed() {
        _state.update { it.copy(event = null) }
    }

    private fun getWalletInfo(walletId: String, receiveAddress: String) {
        getWalletByAddress(receiveAddress)
        getWalletById(walletId)
    }

    fun onShowNoInternetDialogConsumed() {
        _state.update { it.copy(showNoInternet = false) }
    }
}

data class ReceiveWalletTemp(
    val wallet: Wallet,
    val receiveAddress: String
)

data class ReferralInviteFriendUiState(
    val campaign: Campaign? = null,
    val localReferrerCode: ReferrerCode? = null,
    val errorMsg: String? = null,
    val remoteReceiveAddressHash: String? = null,
    val isLoginByEmail: Boolean = false,
    val wallet: Wallet? = null,
    val pickReceiveAddress: String? = null,
    val forceShowInputEmail: Boolean = false,
    val showEmailAlreadyExist: Boolean = false,
    val event: ReferralInviteFriendEvent? = null,
    val receiveWalletTemp: ReceiveWalletTemp? = null,
    val inputEmail: String = "",
    val showNoInternet: Boolean = false
) {
    fun isHideAddress(): Boolean {
        if (localReferrerCode == null) return false
        if (localReferrerCode.receiveAddress.isNotEmpty()) return false
        if (remoteReceiveAddressHash.isNullOrBlank().not()
            && localReferrerCode.receiveAddressHash.isNotEmpty()
            && remoteReceiveAddressHash != localReferrerCode.receiveAddressHash
        ) return true
        return true
    }

    fun getDisplayAddress(): String {
        val address = localReferrerCode?.receiveAddress
        return (if (address.isNullOrEmpty()) pickReceiveAddress else address)?.let {
            simplifyAddress(it)
        }.orEmpty()
    }

    private fun simplifyAddress(address: String): String {
        if (wallet == null) return address
        kotlin.runCatching {
            return address.substring(0, 5) + "..." + address.substring(
                address.length - 4,
                address.length
            )
        }
        return address
    }

    fun getDisplayLink(): String {
        val link = localReferrerCode?.link ?: ""
        val regex = """https?://([^/]+)/.*?([^/]+)$""".toRegex()
        val matchResult = regex.find(link)

        val simplifiedUrl = if (matchResult != null) {
            val (domain, code) = matchResult.destructured
            "$domain/.../$code"
        } else {
            link // Fallback to original URL if parsing fails
        }
        return simplifiedUrl
    }
}

sealed class ReferralInviteFriendEvent {
    data object ChangeAddressSuccess : ReferralInviteFriendEvent()
}