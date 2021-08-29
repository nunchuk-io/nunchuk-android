package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        viewModelScope.launch {
            convertMasterSigners(masterSigners, walletType, addressType)
                .onStart { event(SetLoadingEvent(true)) }
                .flowOn(Dispatchers.IO)
                .map {
                    val signers = it + remoteSigners
                    Timber.d("signers:$signers")
                    draftWalletUseCase.execute(
                        name = walletName,
                        totalRequireSigns = totalRequireSigns,
                        signers = signers,
                        addressType = addressType,
                        isEscrow = walletType == ESCROW
                    )
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
                        isEscrow = walletType == ESCROW
                    )
                }
                .flowOn(Dispatchers.IO)
                .flowOn(Dispatchers.Main)
                .catch {
                    Timber.d("create wallet error:$it")
                    event(CreateWalletErrorEvent(it.message.orUnknownError()))
                }
                .onEach {
                    Timber.d("create wallet completed:$it")
                    event(CreateWalletSuccessEvent(it.id, descriptor))
                }
                .collect {
                    event(SetLoadingEvent(false))
                }
        }
    }

    private fun convertMasterSigners(
        masterSigners: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ) = combine(
        masterSigners.map {
            runBlocking {
                getUnusedSignerUseCase.execute(it.id, walletType, addressType)
            }
        }
    ) { it.toList() }.flowOn(Dispatchers.IO)

}
