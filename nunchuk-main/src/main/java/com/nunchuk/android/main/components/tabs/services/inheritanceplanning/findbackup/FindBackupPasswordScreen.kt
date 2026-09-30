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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceKeyType

@Composable
internal fun FindBackupPasswordContent(
    remainTime: Int = 0,
    inheritanceKeyType: InheritanceKeyType = InheritanceKeyType.TAPSIGNER,
    keyIndex: Int = 1,
    numOfKeys: Int = 1,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = inheritanceKeyType.illustrationRes,
                    title = estimateRemainTimeTitle(remainTime),
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                if (numOfKeys > 1) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_inheritance_key_index, keyIndex, numOfKeys),
                        style = NunchukTheme.typography.title.copy(color = colorResource(id = R.color.nc_beeswax_dark))
                    )
                }
                Text(
                    modifier = Modifier.padding(
                        top = if (numOfKeys > 1) 4.dp else 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = if (inheritanceKeyType == InheritanceKeyType.TAPSIGNER) {
                        stringResource(id = R.string.nc_find_backup_password)
                    } else {
                        stringResource(id = R.string.nc_record_your_backup_password)
                    },
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(
                        inheritanceKeyType.descRes,
                        stringResource(inheritanceKeyType.keyNameRes(keyIndex, numOfKeys)),
                    ),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

/**
 * The body, which takes the key's name as its one argument. TAPSIGNER's password is printed on the
 * card, COLDCARD shows 12 words while the backup file is made, and every other device gets wording
 * that names neither.
 */
@get:StringRes
private val InheritanceKeyType.descRes: Int
    get() = when (this) {
        InheritanceKeyType.TAPSIGNER -> R.string.nc_find_backup_password_desc
        InheritanceKeyType.COLDCARD -> R.string.nc_record_your_backup_password_desc
        InheritanceKeyType.OTHER -> R.string.nc_record_your_backup_password_generic_desc
    }

/**
 * How the body names this key. A plan with a single inheritance key just calls it "the inheritance
 * key"; with two, the key is named by its position and emphasised. The generic body reads "An
 * encrypted backup of <name>", so it takes the possessive form of the same phrase.
 *
 * A plan holds one or two inheritance keys — `THREE_OF_FIVE_INHERITANCE` is the only two-key
 * configuration — so there is no third ordinal to name; a third key would read as the second.
 */
@StringRes
private fun InheritanceKeyType.keyNameRes(keyIndex: Int, numOfKeys: Int): Int {
    val possessive = this == InheritanceKeyType.OTHER
    return when {
        numOfKeys <= 1 ->
            if (possessive) R.string.nc_backup_of_inheritance_key else R.string.nc_designated_inheritance_key

        keyIndex <= 1 ->
            if (possessive) R.string.nc_backup_of_first_inheritance_key else R.string.nc_designated_first_inheritance_key

        else ->
            if (possessive) R.string.nc_backup_of_second_inheritance_key else R.string.nc_designated_second_inheritance_key
    }
}

@get:DrawableRes
private val InheritanceKeyType.illustrationRes: Int
    get() = when (this) {
        InheritanceKeyType.TAPSIGNER -> R.drawable.nc_bg_tap_signer_explain
        InheritanceKeyType.COLDCARD -> R.drawable.bg_backup_coldcard_illustration
        // Neither device is named, so neither is drawn: the app's generic "Backup Password stored
        // on the server" graphic, the same one the share-secrets explainer uses.
        InheritanceKeyType.OTHER -> R.drawable.nc_bg_backup_password_share_secret
    }

@PreviewLightDark
@Composable
private fun FindBackupPasswordSingleTapSignerPreview() {
    FindBackupPasswordContent(inheritanceKeyType = InheritanceKeyType.TAPSIGNER)
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordSingleColdcardPreview() {
    FindBackupPasswordContent(inheritanceKeyType = InheritanceKeyType.COLDCARD)
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordSingleOtherDevicePreview() {
    FindBackupPasswordContent(inheritanceKeyType = InheritanceKeyType.OTHER)
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordFirstOfTwoPreview() {
    FindBackupPasswordContent(
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        keyIndex = 1,
        numOfKeys = 2,
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordSecondOfTwoPreview() {
    FindBackupPasswordContent(
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        keyIndex = 2,
        numOfKeys = 2,
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordSecondOfTwoOtherDevicePreview() {
    FindBackupPasswordContent(
        inheritanceKeyType = InheritanceKeyType.OTHER,
        keyIndex = 2,
        numOfKeys = 2,
    )
}
