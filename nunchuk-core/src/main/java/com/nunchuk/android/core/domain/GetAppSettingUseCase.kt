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

package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.constants.Constants.LIQUID_MAIN_NET_HOST
import com.nunchuk.android.core.constants.Constants.LIQUID_TEST_NET_HOST
import com.nunchuk.android.core.constants.Constants.MAIN_NET_HOST
import com.nunchuk.android.core.constants.Constants.SIG_NET_HOST
import com.nunchuk.android.core.constants.Constants.TEST_NET_HOST
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAppSettingUseCase @Inject constructor(
    private val initAppSettingsUseCase: InitAppSettingsUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, AppSettings>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): AppSettings {
        val rawJson = ncSharedPreferences.appSettings
        val loaded = gson.fromJson(rawJson, AppSettings::class.java)
            ?: return initAppSettingsUseCase(Unit).getOrThrow()

        if (loaded.electrumServers.isEmpty()) {
            val legacy = runCatching {
                gson.fromJson(rawJson, OldAppSettings::class.java)
            }.getOrNull()
            val recoveredHost = legacy?.electrumHostFor(loaded.chain)
                ?: defaultElectrumHost(loaded.chain)
            val migrated = loaded.copy(
                electrumServers = listOf(recoveredHost),
                liquidServers = loaded.liquidServers.ifEmpty {
                    listOf(defaultLiquidHost(loaded.chain))
                },
            )
            return updateAppSettingUseCase(migrated).getOrThrow()
        }
        return loaded
    }

    private fun OldAppSettings.electrumHostFor(chain: Chain): String? {
        val host = when (chain) {
            Chain.MAIN -> mainnetServers.firstOrNull()
            Chain.TESTNET -> testnetServers.firstOrNull()
            Chain.SIGNET -> signetServers.firstOrNull()
            else -> null
        }
        return host?.takeIf { it.isNotBlank() }
    }

    private fun defaultElectrumHost(chain: Chain): String = when (chain) {
        Chain.TESTNET -> TEST_NET_HOST
        Chain.SIGNET -> SIG_NET_HOST
        else -> MAIN_NET_HOST
    }

    private fun defaultLiquidHost(chain: Chain): String = when (chain) {
        Chain.TESTNET, Chain.SIGNET -> LIQUID_TEST_NET_HOST
        else -> LIQUID_MAIN_NET_HOST
    }

    // Mirror of the pre-USDT AppSettings schema used to recover the user's
    // previously-saved Electrum host before set_electrum_servers/set_liquid_servers
    // replaced the per-chain server fields.
    private data class OldAppSettings(
        val mainnetServers: List<String> = emptyList(),
        val testnetServers: List<String> = emptyList(),
        val signetServers: List<String> = emptyList(),
    )
}
