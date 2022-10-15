package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.SINGLE_SIG
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ReviewWalletViewModel @AssistedInject constructor(
    @Assisted private val args: ReviewWalletArgs,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val masterSignerMapper: MasterSignerMapper
) : NunchukViewModel<Unit, ReviewWalletEvent>() {

    override val initialState = Unit

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        masterSigners: List<SingleSigner>,
        remoteSigners: List<SingleSigner>
    ) {
        val totalSigns = masterSigners.size + remoteSigners.size
        val normalizeWalletType =
            if (walletType == ESCROW) ESCROW else if (totalSigns > 1) WalletType.MULTI_SIG else SINGLE_SIG
        viewModelScope.launch {
            flowOf(masterSigners)
                .flowOn(Dispatchers.IO)
                .onStart { event(SetLoadingEvent(true)) }
                .map {
                    val signers = it + remoteSigners
                    draftWalletUseCase.execute(
                        name = walletName,
                        totalRequireSigns = totalRequireSigns,
                        signers = signers,
                        addressType = addressType,
                        isEscrow = normalizeWalletType == ESCROW
                    )
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
                .onException {
                    event(CreateWalletErrorEvent(it.message.orUnknownError()))
                }
                .collect {
                    Timber.d("create wallet completed:$it")
                    event(CreateWalletSuccessEvent(it.id))
                }
        }
    }

    fun mapSigners(): List<SignerModel> {
        return args.masterSigners.map(SingleSigner::toModel) + args.remoteSigners.map(SingleSigner::toModel)
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: ReviewWalletArgs): ReviewWalletViewModel
    }

}
