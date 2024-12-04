package com.nunchuk.android.signer.mk4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SaveMembershipExistingColdCardUseCase
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4ViewModel @Inject constructor(
    private val saveMembershipExistingColdCardUseCase: SaveMembershipExistingColdCardUseCase,
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<Mk4Event>()
    val event = _event.asSharedFlow()

    lateinit var coldCardBackUpParam: ColdCardBackUpParam
        private set

    fun setOrUpdate(param: ColdCardBackUpParam) {
        coldCardBackUpParam = param
    }

    fun saveMembershipExistingColdCard() = viewModelScope.launch {
        _event.emit(Mk4Event.Loading(true))
        saveMembershipExistingColdCardUseCase(
            SaveMembershipExistingColdCardUseCase.Params(
                xfp = coldCardBackUpParam.xfp,
                newIndex = 0,
                step = membershipStepManager.currentStep ?: throw IllegalArgumentException("Current step empty"),
                plan = membershipStepManager.localMembershipPlan,
                groupId = coldCardBackUpParam.groupId
            )
        ).onSuccess {
            _event.emit(Mk4Event.Success)
        }
        _event.emit(Mk4Event.Loading(false))
    }
}

sealed class Mk4Event {
    data class Loading(val isLoading: Boolean) : Mk4Event()
    data object Success : Mk4Event()
}

data class ColdCardBackUpParam(
    val isHasPassphrase: Boolean = false,
    val xfp: String,
    val keyType: SignerType,
    val filePath: String,
    val keyName: String,
    val backUpFileName: String,
    val keyId: String,
    val isRequestAddOrReplaceKey: Boolean,
    val groupId: String = "",
)