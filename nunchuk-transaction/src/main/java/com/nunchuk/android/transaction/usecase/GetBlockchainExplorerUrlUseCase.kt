package com.nunchuk.android.transaction.usecase

import com.nunchuk.android.core.constants.Constants.GLOBAL_SIGNET_EXPLORER
import com.nunchuk.android.core.constants.Constants.MAINNET_URL_TEMPLATE
import com.nunchuk.android.core.constants.Constants.TESTNET_URL_TEMPLATE
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun Chain.isMainNet() = this == Chain.MAIN

interface GetBlockchainExplorerUrlUseCase {
    fun execute(txId: String): Flow<String>
}

internal class GetBlockchainExplorerUrlUseCaseImpl @Inject constructor(
    private val appSettingsUseCase: GetAppSettingUseCase
) : GetBlockchainExplorerUrlUseCase {

    override fun execute(txId: String) = appSettingsUseCase.execute().map { formatUrl(it.chain, txId, it.signetExplorerHost) }

    private fun formatUrl(chain: Chain, txId: String, signetExplorerHost: String) = getTemplate(chain, signetExplorerHost) + txId

    private fun getTemplate(chain: Chain, signetExplorerHost: String) = when (chain) {
        Chain.MAIN -> MAINNET_URL_TEMPLATE
        Chain.TESTNET -> TESTNET_URL_TEMPLATE
        Chain.SIGNET -> if(signetExplorerHost.isEmpty()) "$GLOBAL_SIGNET_EXPLORER/tx/" else "$signetExplorerHost/tx/"
        else -> ""
    }

}

