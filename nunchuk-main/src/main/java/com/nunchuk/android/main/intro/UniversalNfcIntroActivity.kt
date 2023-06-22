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

package com.nunchuk.android.main.intro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UniversalNfcIntroActivity : AppCompatActivity() {
    private val viewModel: UniversalNfcIntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            UniversalNfcIntroScreen(viewModel)
        }

        flowObserver(viewModel.event) {
            when(it) {
                UniversalNfcIntroEvent.OnGotItClicked -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    companion object {
        fun navigate(launcher: ActivityResultLauncher<Intent>, activity: Activity) {
            launcher.launch(Intent(activity, UniversalNfcIntroActivity::class.java))
        }
    }
}

@Composable
private fun UniversalNfcIntroScreen(viewModel: UniversalNfcIntroViewModel = viewModel()) = NunchukTheme {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(backgroundRes = R.drawable.nc_bg_universal_nfc_intro)
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_locate_phone_nfc),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_universal_nfc_intro_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    onClick = { viewModel.onGotInClicked() }) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        }
    }
}

@Preview
@Composable
private fun UniversalNfcIntroScreenPreview() {
    UniversalNfcIntroScreen()
}