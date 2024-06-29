package com.nunchuk.android.signer.software

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoftwareSignerIntroViewModel @Inject constructor(
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _state = MutableStateFlow(SoftwareSignerIntroState())
    val state = _state.asStateFlow()

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
}

data class SoftwareSignerIntroState(
    val replaceSignerName: String = ""
)