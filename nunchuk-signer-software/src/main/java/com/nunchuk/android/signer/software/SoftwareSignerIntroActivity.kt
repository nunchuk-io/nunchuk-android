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

package com.nunchuk.android.signer.software

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.util.ClickAbleText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoftwareSignerIntroActivity : BaseComposeActivity() {

    private val primaryKeyFlow: Int by lazy {
        intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
    }
    private val passphrase: String by lazy {
        intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
    }
    private val groupId: String? by lazy {
        intent.getStringExtra(EXTRA_GROUP_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NunchukTheme {
                SoftwareSignerIntroScreen(
                    onCreateNewSeedClicked = ::openCreateNewSeedScreen,
                    onRecoverSeedClicked = ::openRecoverSeedScreen
                )
            }
        }
    }

    private fun openCreateNewSeedScreen() {
        navigator.openCreateNewSeedScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow,
            groupId = groupId,
            replacedXfp = replacedXfp
        )
    }

    private fun openRecoverSeedScreen() {
        navigator.openRecoverSeedScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow,
            groupId = groupId,
            replacedXfp = replacedXfp
        )
    }

    val replacedXfp: String? by lazy {
        intent.getStringExtra(EXTRA_REPLACED_XFP)
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_REPLACED_XFP = "EXTRA_REPLACED_XFP"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun start(
            activityContext: Context,
            passphrase: String,
            primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
            groupId: String? = null,
            replacedXfp: String? = null,
            walletId: String = "",
        ) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SoftwareSignerIntroActivity::class.java
                ).apply {
                    putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
                    putExtra(EXTRA_PASSPHRASE, passphrase)
                    groupId?.let { putExtra(EXTRA_GROUP_ID, it) }
                    replacedXfp?.let { putExtra(EXTRA_REPLACED_XFP, it) }
                    putExtra(EXTRA_WALLET_ID, walletId)
                },
            )
        }
    }
}

@Composable
fun SoftwareSignerIntroScreen(
    onCreateNewSeedClicked: () -> Unit = {},
    onRecoverSeedClicked: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_before_you_start),
                textStyle = NunchukTheme.typography.titleLarge,
                actions = {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NcHintMessage(
                    messages = listOf(ClickAbleText("Upgrade software keys to hardware keys for improved security."))
                )

                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateNewSeedClicked
                ) {
                    Text(text = stringResource(id = R.string.nc_ssigner_new_seed))
                }

                NcOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRecoverSeedClicked
                ) {
                    Text(text = stringResource(id = R.string.nc_ssigner_recover_seed))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            NcCircleImage(
                resId = R.drawable.ic_warning_outline,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally),
                size = 96.dp,
                iconSize = 60.dp
            )

            NcSpannedText(
                modifier = Modifier.padding(top = 24.dp),
                text = "A software key will be generated locally on this device. [B]Deleting the app will also delete the software key.[/B]\n\nPlease make sure to:",
                baseStyle = NunchukTheme.typography.body,
                styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold)),
            )

            Section(
                modifier = Modifier.padding(top = 24.dp),
                iconResId = R.drawable.ic_replace_primary_key,
                title = "Back up the key ",
                content = "The backup will allow you to recover the key in worst case scenarios.",
            )

            Section(
                modifier = Modifier.padding(top = 24.dp),
                iconResId = R.drawable.ic_emergency_lockdown_dark,
                title = "Keep your device secure",
                content = "Since the software key resides on this device, keeping the device safe will prevent the software key from being compromised.",
            )
        }
    }
}

@Composable
private fun Section(
    @DrawableRes iconResId: Int,
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painter = painterResource(id = iconResId), contentDescription = "Icon")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.title,
            )
            Text(
                text = content,
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@Preview
@Composable
private fun SoftwareSignerIntroPreview() {
    NunchukTheme {
        SoftwareSignerIntroScreen()
    }
}
