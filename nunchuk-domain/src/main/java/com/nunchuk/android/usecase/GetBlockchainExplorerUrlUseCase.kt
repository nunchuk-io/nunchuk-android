package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

const val URL_TEMPLATE = "https://blockstream.info/%1s/tx/%2s"

interface GetBlockchainExplorerUrlUseCase {
    fun execute(txId: String): Flow<String>
}

internal class GetBlockchainExplorerUrlUseCaseImpl @Inject constructor(
    private val appSettingsUseCase: GetAppSettingsUseCase
) : GetBlockchainExplorerUrlUseCase {

    override fun execute(txId: String) = appSettingsUseCase.execute().map { formatUrl(it, txId) }

    private fun formatUrl(
        settings: AppSettings,
        txId: String
    ) = String.format(URL_TEMPLATE, (if (settings.chain == Chain.TESTNET) "testnet" else "main"), txId)

}

