package com.nunchuk.android.transaction.usecase

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

const val TESTNET_URL_TEMPLATE = "https://blockstream.info/testnet/tx/"
const val MAINNET_URL_TEMPLATE = "https://blockstream.info/tx/"

internal fun Chain.isMainNet() = this == Chain.MAIN

interface GetBlockchainExplorerUrlUseCase {
    fun execute(txId: String): Flow<String>
}

internal class GetBlockchainExplorerUrlUseCaseImpl @Inject constructor(
    private val appSettingsUseCase: GetAppSettingUseCase
) : GetBlockchainExplorerUrlUseCase {

    override fun execute(txId: String) = appSettingsUseCase.execute().map { formatUrl(it.chain, txId) }

    private fun formatUrl(chain: Chain, txId: String) = getTemplate(chain) + txId

    private fun getTemplate(chain: Chain) = if (chain.isMainNet()) {
        MAINNET_URL_TEMPLATE
    } else {
        (TESTNET_URL_TEMPLATE)
    }

}

