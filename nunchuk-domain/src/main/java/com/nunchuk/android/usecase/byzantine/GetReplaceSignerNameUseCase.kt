package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetReplaceSignerNameUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository,
) : UseCase<GetReplaceSignerNameUseCase.Params, String>(dispatcher) {

    override suspend fun execute(parameters: Params): String {
        return repository.getReplaceSignerName(
            parameters.walletId,
            parameters.signerType,
            parameters.tag
        )
    }

    data class Params(
        val walletId: String,
        val signerType: SignerType,
        val tag: SignerTag? = null,
    )
}