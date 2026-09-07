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

package com.nunchuk.android.signer.util

import androidx.annotation.StringRes
import com.nunchuk.android.core.util.generateUniqueSignerName
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.usecase.CreateSignerUseCase

fun isTestNetPath(path: String): Boolean {
    return path.split("/").getOrNull(2) == "1h"
}

/**
 * Heading for the air-gapped add-key screens. The screens after the key-type picker know which
 * device the user chose, so they name it; a generic air-gap add has no device to name.
 */
@StringRes
fun SignerTag?.airgapAddKeyTitleRes(): Int = when (this) {
    SignerTag.SEEDSIGNER -> R.string.nc_add_seedsigner
    SignerTag.JADE -> R.string.nc_add_jade
    SignerTag.PASSPORT -> R.string.nc_add_foundation_passport
    SignerTag.KEYSTONE -> R.string.nc_add_keystone
    SignerTag.KRUX -> R.string.nc_add_krux
    else -> R.string.nc_add_an_airgapped_key
}

/**
 * Persists every account read off one hardware device in a single session. Ledger and BitBox can
 * hand over several accounts per connection, which is what a miniscript "Reuse keys across
 * policies" slot needs — one xpub per policy, off the same device, without pairing twice.
 *
 * The first account keeps [name] (what the user typed, or the auto-generated name in a membership
 * flow); the accounts behind it step up to "<name> 2", "<name> 3"… so they collide neither with
 * [takenNames] nor with each other. [onCreated] is called per key, so a wallet flow waiting on
 * the key hears about all of them; the first key is returned for the caller's "key added" step.
 */
internal suspend fun createDeviceAccountKeys(
    accounts: List<SingleSigner>,
    name: String,
    replace: Boolean,
    takenNames: List<String>,
    createSignerUseCase: CreateSignerUseCase,
    onCreated: suspend (SingleSigner) -> Unit,
): Result<SingleSigner> {
    val names = takenNames.toMutableList()
    var firstCreated: SingleSigner? = null
    accounts.forEachIndexed { position, account ->
        val signerName = if (position == 0) name else generateUniqueSignerName(name, names)
        val created = createSignerUseCase(
            CreateSignerUseCase.Params(
                name = signerName,
                xpub = account.xpub,
                type = account.type,
                derivationPath = account.derivationPath,
                masterFingerprint = account.masterFingerprint,
                tags = account.tags,
                replace = replace,
            )
        ).getOrElse { return Result.failure(it) }
        names += created.name
        onCreated(created)
        if (firstCreated == null) firstCreated = created
    }
    return firstCreated?.let { Result.success(it) }
        ?: Result.failure(IllegalStateException("No key was read from the device"))
}
