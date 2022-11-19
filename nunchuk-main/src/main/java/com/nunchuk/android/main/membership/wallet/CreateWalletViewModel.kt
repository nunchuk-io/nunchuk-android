package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.GetDefaultSignerFromMasterSignerUseCase
import com.nunchuk.android.core.domain.membership.CreateServerWalletUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.ServerKeyExtra
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWalletViewModel @Inject constructor(
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val getMembershipStepUseCase: GetMembershipStepUseCase,
    private val createSignerUseCase: CreateSignerUseCase,
    private val gson: Gson,
    private val createServerWalletUseCase: CreateServerWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {
    private val masterSignerIds = mutableListOf<String>()
    private var serverKeyExtra: ServerKeyExtra? = null
    private var serverKeyId: String? = null

    private val _event = MutableSharedFlow<CreateWalletEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CreateWalletState.EMPTY)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getMembershipStepUseCase(membershipStepManager.plan)
                .filter { it.isSuccess }
                .map { it.getOrThrow() }
                .collect { steps ->
                    steps.forEach {
                        when (it.step) {
                            MembershipStep.ADD_TAP_SIGNER_1,
                            MembershipStep.ADD_TAP_SIGNER_2 -> masterSignerIds.add(it.masterSignerId)
                            MembershipStep.ADD_SEVER_KEY -> {
                                serverKeyExtra = runCatching {
                                    gson.fromJson(
                                        it.extraData,
                                        ServerKeyExtra::class.java
                                    )
                                }.getOrNull()
                                serverKeyId = it.keyIdInServer
                            }
                            else -> {}
                        }
                    }
                }
        }
    }

    fun updateWalletName(walletName: String) {
        _state.update {
            it.copy(walletName = walletName)
        }
    }

    fun onContinueClicked() {
        createQuickWallet()
    }

    private fun createQuickWallet() {
        val serverKey = serverKeyExtra ?: return
        val serverKeyId = serverKeyId ?: return
        viewModelScope.launch {
            val addressType = AddressType.NATIVE_SEGWIT
            _event.emit(CreateWalletEvent.Loading(true))
            val getSingleSingerResult = getDefaultSignerFromMasterSignerUseCase(
                GetDefaultSignerFromMasterSignerUseCase.Params(
                    masterSignerIds,
                    WalletType.MULTI_SIG,
                    addressType
                )
            )
            if (getSingleSingerResult.isFailure) {
                _event.emit(
                    CreateWalletEvent.ShowError(
                        getSingleSingerResult.exceptionOrNull()?.message.orUnknownError()
                    )
                )
                _event.emit(CreateWalletEvent.Loading(false))
                return@launch
            }
            val createServerSignerResult = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = serverKey.name,
                    xpub = serverKey.xpub,
                    derivationPath = serverKey.derivationPath,
                    masterFingerprint = serverKey.xfp,
                    type = SignerType.SERVER
                )
            )
            if (createServerSignerResult.isFailure) {
                _event.emit(
                    CreateWalletEvent.ShowError(
                        createServerSignerResult.exceptionOrNull()?.message.orUnknownError()
                    )
                )
                _event.emit(CreateWalletEvent.Loading(false))
                return@launch
            }
            createWalletUseCase.execute(
                name = _state.value.walletName,
                totalRequireSigns = 2,
                signers = getSingleSingerResult.getOrThrow() + createServerSignerResult.getOrThrow(),
                addressType = addressType,
                isEscrow = false
            ).map {
                val result = createServerWalletUseCase(
                    CreateServerWalletUseCase.Params(it, serverKeyId, membershipStepManager.plan)
                )
                if (result.isFailure) {
                    deleteWalletUseCase.execute(it.id)
                }
                it
            }.flowOn(Dispatchers.IO)
                .flowOn(Dispatchers.Main)
                .onCompletion { _event.emit(CreateWalletEvent.Loading(false)) }
                .onException {
                    _event.emit(CreateWalletEvent.ShowError(it.message.orUnknownError()))
                }
                .collect {
                    _event.emit(CreateWalletEvent.OnCreateWalletSuccess(it.id))
                }
        }
    }
}