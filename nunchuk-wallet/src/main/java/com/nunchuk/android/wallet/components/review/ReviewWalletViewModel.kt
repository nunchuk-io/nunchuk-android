package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.SINGLE_SIG
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class ReviewWalletViewModel @Inject constructor(
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase
) : NunchukViewModel<Unit, ReviewWalletEvent>() {

    override val initialState = Unit

    private var descriptor = ""

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ) {
        val totalSigns = masterSigners.size + remoteSigners.size
        val normalizeWalletType = if (walletType == ESCROW) ESCROW else if (totalSigns > 1) WalletType.MULTI_SIG else SINGLE_SIG
        viewModelScope.launch {
            getUnusedSignerUseCase.execute(masterSigners, normalizeWalletType, addressType)
                .flowOn(Dispatchers.IO)
                .onStart { event(SetLoadingEvent(true)) }
                .map {
                    val signers = it + remoteSigners
                    Timber.d("signers:$signers")
                    draftWalletUseCase.execute(
                        name = walletName,
                        totalRequireSigns = totalRequireSigns,
                        signers = signers,
                        addressType = addressType,
                        isEscrow = normalizeWalletType == ESCROW
                    ).onEach { s -> descriptor = s }
                    Timber.d("descriptor:$descriptor")
                    signers
                }
                .flowOn(Dispatchers.IO)
                .flatMapMerge {
                    createWalletUseCase.execute(
                        name = walletName,
                        totalRequireSigns = totalRequireSigns,
                        signers = it,
                        addressType = addressType,
                        isEscrow = normalizeWalletType == ESCROW
                    )
                }
                .flowOn(Dispatchers.Main)
                .catch {
                    Timber.d("create wallet error:$it")
                    event(CreateWalletErrorEvent(it.message.orUnknownError()))
                }
                .collect {
                    Timber.d("create wallet completed:$it")
                    event(CreateWalletSuccessEvent(it.id, descriptor))
                }
        }
    }

}
