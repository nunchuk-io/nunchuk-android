/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.usecase

import com.nunchuk.android.core.constants.Constants.GLOBAL_SIGNET_EXPLORER
import com.nunchuk.android.core.constants.Constants.MAINNET_URL_TEMPLATE
import com.nunchuk.android.core.constants.Constants.TESTNET_URL_TEMPLATE
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.settings.GetCustomExplorerUrlFlowUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetBlockchainExplorerUrlUseCase @Inject constructor(
    private val appSettingsUseCase: GetAppSettingUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val getCustomExplorerUrlFlowUseCase: GetCustomExplorerUrlFlowUseCase,
) : UseCase<String, String>(dispatcher) {

    override suspend fun execute(parameters: String): String {
        val settings = appSettingsUseCase(Unit).getOrThrow()
        return formatUrl(settings.chain, parameters, settings.signetExplorerHost)
    }

    private suspend fun formatUrl(chain: Chain, txId: String, signetExplorerHost: String) =
        getCustomUrl(chain, signetExplorerHost) + txId

    private suspend fun getCustomUrl(chain: Chain, signetExplorerHost: String) =
        getCustomExplorerUrlFlowUseCase(Unit)
            .map { result ->
                val custom = result.getOrNull()?.takeIf { it.isNotEmpty() }
                custom?.let { it.substringBeforeLast("tx") + "tx/" }
            }.first().takeIf { !it.isNullOrEmpty() }
            ?: getTemplate(chain, signetExplorerHost)

    private fun getTemplate(chain: Chain, signetExplorerHost: String) = when (chain) {
        Chain.MAIN -> MAINNET_URL_TEMPLATE
        Chain.TESTNET -> TESTNET_URL_TEMPLATE
        Chain.SIGNET -> if (signetExplorerHost.isEmpty()) "$GLOBAL_SIGNET_EXPLORER/tx/" else "$signetExplorerHost/tx/"
        else -> ""
    }

}

