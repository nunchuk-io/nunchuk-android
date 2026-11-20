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

package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.type.WalletType
import kotlinx.parcelize.Parcelize

@Parcelize
data class InheritanceClaimingInit(
    val walletType: WalletType,
    val walletLocalId: String,
    val inheritanceKeyCount: Int,
    val keyOrigins: List<KeyOrigin> = emptyList(),
    /**
     * Distinct numbers of independent keys that can claim, one per claim condition (`[2]`, or `[1, 2]`
     * when a fallback needs fewer keys than the primary), ascending. Empty from a backend that does
     * not send `required_key_counts`; only [inheritanceKeyCount] applies then.
     */
    val requiredKeyCounts: List<Int> = emptyList(),
    /**
     * `show_seed_phrase_guidance`: whether a claim of 2+ keys first recommends separate devices
     * (CLM-KCa). False when the plan's keys only have a Backup Password (TAPSIGNER). Null from a
     * backend that does not send it.
     */
    val showSeedPhraseGuidance: Boolean? = null,
) : Parcelable

@Parcelize
data class KeyOrigin(
    val xfp: String,
    val derivationPath: String
) : Parcelable

