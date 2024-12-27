package com.nunchuk.android.main.membership

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>(MembershipFragment.EXTRA_GROUP_ID).orEmpty()
    private val walletId =
        savedStateHandle.get<String>(MembershipActivity.EXTRA_KEY_WALLET_ID).orEmpty()

    private val _state = MutableStateFlow(
        MembershipState(groupId = savedStateHandle.get<String>(MembershipFragment.EXTRA_GROUP_ID).orEmpty())
    )
    val state = _state.asStateFlow()

    init {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupUseCase(GetGroupUseCase.Params(groupId))
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .collect { groupBrief ->
                        _state.update { it.copy(groupWalletType = groupBrief.walletConfig.toGroupWalletType()) }
                    }
            }
        }
        if (walletId.isNotEmpty()) {
            viewModelScope.launch {
                val result = getWalletDetail2UseCase(walletId)
                if (result.isSuccess) {
                    val wallet = result.getOrThrow()
                    if (wallet.addressType.isTaproot()) {
                        getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
                            _state.update { it.copy(supportedTypes = supportedTypes) }
                        }
                    }
                }
            }
        }
    }

    fun getSupportedSigners() = _state.value.supportedTypes
}

data class MembershipState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
    val supportedTypes: List<SupportedSigner> = emptyList()
)