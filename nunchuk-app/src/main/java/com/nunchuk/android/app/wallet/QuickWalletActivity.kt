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

package com.nunchuk.android.app.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickWalletActivity : BaseComposeActivity() {

    private val quickWalletParam by lazy {
        intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuickWalletScreen(
                    onContinueClicked = {
                        navigator.openHotWalletScreen(
                            launcher = null,
                            activityContext = this@QuickWalletActivity,
                            quickWalletParam = quickWalletParam
                        )
                    },
                    onCreateOwnWalletClicked = {
                        navigator.openWalletIntermediaryScreen(
                            activityContext = this@QuickWalletActivity,
                            quickWalletParam = quickWalletParam,
                        )
                    }
                )
            }
        })
    }

    companion object {

        private const val EXTRA_QUICK_WALLET_PARAM = "extra_quick_wallet_param"

        fun navigate(context: Context, quickWalletParam: QuickWalletParam?) {
            context.startActivity(Intent(context, QuickWalletActivity::class.java).apply {
                putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
            })
        }
    }
}

@Composable
fun QuickWalletScreen(
    onContinueClicked: () -> Unit = {},
    onCreateOwnWalletClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    isBack = false
                )
            }, bottomBar = {
                Column {

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = {
                            onContinueClicked()
                        },
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }

                    NcOutlineButton(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        onClick = onCreateOwnWalletClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_create_my_own_wallet))
                    }
                }

            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_circle_new_wallet),
                        contentDescription = "Help Icon",
                        modifier = Modifier.size(96.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.nc_you_dont_have_a_wallet_yet),
                    style = NunchukTheme.typography.heading,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.nc_you_dont_have_a_wallet_yet_desc),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewQuickWalletScreen() {
    QuickWalletScreen(
    )
}