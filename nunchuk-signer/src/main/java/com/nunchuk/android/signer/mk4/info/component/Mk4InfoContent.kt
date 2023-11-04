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

package com.nunchuk.android.signer.mk4.info.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.R

@Composable
internal fun Mk4InfoContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onOpenGuideClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    isMembershipFlow: Boolean = true,
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                    title = if (isMembershipFlow) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    actions = {
                        if (isMembershipFlow) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    }
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_add_your_coldcard),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_ensure_to_following_step),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = R.string.nc_init_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    NcClickableText(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        messages = listOf(
                            ClickAbleText(content = stringResource(id = R.string.nc_refer_to)),
                            ClickAbleText(
                                content = stringResource(id = R.string.nc_this_starter_guide),
                                onOpenGuideClicked
                            )
                        ),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = R.string.nc_unlock_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_unlock_device_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 3,
                    title = stringResource(id = R.string.nc_enable_nfc),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_enable_mk4_nfc_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(R.string.nc_add_coldcard_from_file_hint)
                        )
                    )
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun Mk4InfoContentPreview() {
    NunchukTheme {
        Mk4InfoContent()
    }
}