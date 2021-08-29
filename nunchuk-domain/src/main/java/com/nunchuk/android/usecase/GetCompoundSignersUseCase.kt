package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

interface GetCompoundSignersUseCase {
    fun execute(): Flow<Pair<List<MasterSigner>, List<SingleSigner>>>
}

internal class GetCompoundSignersUseCaseImpl @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase
) : GetCompoundSignersUseCase {

    override fun execute() = getMasterSignersUseCase.execute().zip(getRemoteSignersUseCase.execute()) { masters, remotes ->
        masters to remotes
    }

}