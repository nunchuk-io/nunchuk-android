package com.nunchuk.android.utils

import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.SendMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import javax.inject.Inject

class GroupChatManager @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val sendMessageFreeGroupWalletUseCase: SendMessageFreeGroupWalletUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
) {

    var selectedSigner: SingleSigner? = null
        private set

    suspend fun init(walletId: String) {
        getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
            val signers = mapSigners(singleSigners, masterSigners)
            getWalletDetail(walletId, signers)
        }
    }

    private suspend fun getWalletDetail(walletId: String, allSigner: List<SignerModel>) {
        if (walletId.isEmpty()) return
        getWalletDetail2UseCase(walletId).onSuccess { wallet ->
            val signers = wallet.signers
            selectedSigner = signers.find { signer ->
                allSigner.any { it.fingerPrint == signer.masterFingerprint && it.type != SignerType.UNKNOWN }
            }
            if (selectedSigner == null) {
                signers.filter { it.type != SignerType.UNKNOWN  }.forEach { signer ->
                    if (hasSignerUseCase(signer).isSuccess) {
                        selectedSigner = signer
                        return@forEach
                    }
                }
            }
        }.onFailure {
            // Handle failure
        }
    }

    suspend fun sendMessage(message: String, walletId: String, onError: (Throwable) -> Unit) {
        var lastError: Throwable? = null
        
        repeat(MAX_RETRIES) { attempt ->
            sendMessageFreeGroupWalletUseCase(
                SendMessageFreeGroupWalletUseCase.Param(
                    walletId = walletId,
                    message = message,
                    singleSigner = selectedSigner,
                )
            ).onSuccess {
                return
            }.onFailure { throwable ->
                lastError = throwable
                val errorCode = throwable.nativeErrorCode()
                if (errorCode == GROUP_NOT_ENABLED_ERROR_CODE) {
                    initNunchukUseCase.retryInitGroupWallet()
                } else {
                    onError(throwable)
                    return
                }
            }
        }
        
        lastError?.let { error -> onError(error) }
    }

    companion object {
        private const val GROUP_NOT_ENABLED_ERROR_CODE = -7000
        private const val MAX_RETRIES = 3
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>, masterSigners: List<MasterSigner>,
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }
}