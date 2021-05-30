package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.type.Chain
import javax.inject.Inject

const val URL_TEMPLATE = "https://blockstream.info/%1s/tx/%2s"

interface GetBlockchainExplorerUrlUseCase {
    suspend fun execute(txId: String): Result<String>
}

internal class GetBlockchainExplorerUrlUseCaseImpl @Inject constructor(
    private val appSettingsUseCase: GetAppSettingsUseCase
) : BaseUseCase(), GetBlockchainExplorerUrlUseCase {

    override suspend fun execute(txId: String) = exe {
        when (val result = appSettingsUseCase.execute()) {
            is Success -> formatUrl(result, txId)
            is Error -> throw Exception(result.exception)
        }
    }

    private fun formatUrl(
        result: Success<AppSettings>,
        txId: String
    ) = String.format(URL_TEMPLATE, (if (result.data.chain == Chain.TESTNET) "testnet" else "main"), txId)

}