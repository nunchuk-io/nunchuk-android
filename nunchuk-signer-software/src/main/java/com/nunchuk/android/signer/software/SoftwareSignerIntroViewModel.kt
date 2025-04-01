package com.nunchuk.android.signer.software

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GenerateMnemonicUseCase
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.signer.GetMasterSigners2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoftwareSignerIntroViewModel @Inject constructor(
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val generateMnemonicUseCase: GenerateMnemonicUseCase,
    private val getMasterSigners2UseCase: GetMasterSigners2UseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SoftwareSignerIntroState())
    val state = _state.asStateFlow()

    private val _hotKeyInfo = MutableStateFlow(Pair("", ""))
    val hotKeyInfo = _hotKeyInfo.asStateFlow()

    fun getReplaceSignerName(walletId: String) {
        viewModelScope.launch {
            getReplaceSignerNameUseCase(
                GetReplaceSignerNameUseCase.Params(
                    walletId = walletId,
                    signerType = SignerType.SOFTWARE
                )
            ).onSuccess { name ->
                _state.update { it.copy(replaceSignerName = name) }
            }
        }
    }

    fun getSoftwareSignerName() = membershipStepManager.getNextKeySuffixByType(SignerType.SOFTWARE)

    fun getHotKeyInfo(isAssistedWallet: Boolean) {
        viewModelScope.launch {
            val mnemonicHotKey = generateMnemonicUseCase(24).getOrDefault("")
            val masterSigners =
                getMasterSigners2UseCase(Unit).getOrDefault(emptyList()).filter { it.isNeedBackup }
            val hotKeyName = if (masterSigners.isNotEmpty()) {
                "My key #${masterSigners.size + 1}"
            } else {
                "My key"
            }
            _hotKeyInfo.update {
                Pair(
                    mnemonicHotKey,
                    hotKeyName
                )
            }
        }
    }

    fun clearHotKeyInfo() {
        _hotKeyInfo.update {
            Pair(
                "",
                ""
            )
        }
    }
}

data class SoftwareSignerIntroState(
    val replaceSignerName: String = ""
)