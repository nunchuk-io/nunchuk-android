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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.countWords
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InheritanceClaimInputFragment : Fragment() {
    private val viewModel: InheritanceClaimInputViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceClaimScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceClaimInputEvent.Error -> showError(message = event.message)
                is InheritanceClaimInputEvent.GetInheritanceStatusSuccess -> {
                    if (event.inheritanceAdditional.bufferPeriodCountdown == null) {
                        findNavController().navigate(
                            InheritanceClaimInputFragmentDirections.actionInheritanceClaimInputFragmentToInheritanceClaimNoteFragment(
                                signers = event.signers.toTypedArray(),
                                magic = event.magic,
                                inheritanceAdditional = event.inheritanceAdditional,
                                derivationPaths = event.derivationPaths.toTypedArray()
                            )
                        )
                    } else {
                        findNavController().navigate(
                            InheritanceClaimInputFragmentDirections.actionInheritanceClaimInputFragmentToInheritanceClaimBufferPeriodFragment(
                                countdownBufferPeriod = event.inheritanceAdditional.bufferPeriodCountdown!!
                            )
                        )
                    }
                }

                is InheritanceClaimInputEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is InheritanceClaimInputEvent.SubscriptionExpired -> showSubscriptionExpiredDialog()
                is InheritanceClaimInputEvent.InActivated -> showInActivatedDialog(event.message)
                is InheritanceClaimInputEvent.PleaseComeLater -> showPleaseComeLaterDialog(event.message)
                is InheritanceClaimInputEvent.SecurityDepositRequired -> showSecurityDepositRequiredDialog(event.message)
            }
        }
    }

    private fun showSubscriptionExpiredDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_expired_inheritance_subscription),
            btnYes = getString(R.string.nc_take_me_reactivate_plan),
            btnInfo = getString(R.string.nc_text_do_this_later),
            onYesClick = {
                val link =
                    if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/claim" else "https://www.nunchuk.io/claim"
                requireActivity().openExternalLink(link)
            }
        )
    }

    private fun showInActivatedDialog(message: String) {
        NCInfoDialog(requireActivity()).showDialog(message = message)
    }

    private fun showPleaseComeLaterDialog(message: String) {
        NCInfoDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_please_come_back_later),
            message = message,
            btnYes = getString(R.string.nc_text_got_it)
        )
    }

    private fun showSecurityDepositRequiredDialog(message: String) {
        NCInfoDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_security_deposit_required),
            message = message,
            btnYes = getString(R.string.nc_go_to_website_to_deposit),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                val link =
                    if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/claim" else "https://www.nunchuk.io/claim"
                requireActivity().openExternalLink(link)
            }
        )
    }
}

@Composable
fun InheritanceClaimScreen(
    viewModel: InheritanceClaimInputViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InheritanceClaimInputContent(suggestions = state.suggestions,
        magicalPhrase = state._magicalPhrase,
        backupDownloads = state._backupPasswords,
        onMagicalPhraseTextChange = {
            viewModel.handleInputEvent(it.lowercase())
        }, onSuggestClick = {
            viewModel.handleSelectWord(it)
        }, onBackupDownloadTextChange = { text, index ->
            viewModel.updateBackupPassword(text, index)
        }, onContinueClick = {
            viewModel.downloadBackupKey()
        })
}

@Composable
private fun InheritanceClaimInputContent(
    magicalPhrase: String = "",
    backupDownloads: List<String> = emptyList(),
    suggestions: List<String> = emptyList(),
    onContinueClick: () -> Unit = {},
    onSuggestClick: (String) -> Unit = {},
    onMagicalPhraseTextChange: (String) -> Unit = {},
    onBackupDownloadTextChange: (String, Int) -> Unit = { _, _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {

            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = magicalPhrase.countWords() >= 1 && backupDownloads.any { it.countWords() >= 1 },
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = lazyListState
            ) {
                item {
                    NcImageAppBar(
                        backgroundRes = R.drawable.bg_claim_inheritance_illustration,
                        title = "",
                    )
                    Text(
                        modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_claim_inheritance),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        text = stringResource(R.string.nc_claim_inheritance_desc),
                        style = NunchukTheme.typography.body
                    )
                    NcTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .padding(horizontal = 16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        title = stringResource(id = R.string.nc_magical_phrase),
                        value = TextFieldValue(
                            text = magicalPhrase,
                            selection = TextRange(magicalPhrase.length)
                        ),
                        onValueChange = {
                            onMagicalPhraseTextChange(it.text)
                        },
                        onFocusEvent = { isFocused ->
                            if (isFocused) {
                                coroutineScope.launch {
                                    delay(100)
                                    lazyListState.animateScrollToItem(index = 2)
                                }
                            }
                        }
                    )
                }
                item {
                    LazyRow {
                        items(suggestions) {
                            Card(
                                modifier = Modifier
                                    .padding(vertical = 16.dp, horizontal = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    onSuggestClick(it)
                                }
                            ) {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    backupDownloads.forEachIndexed { index, backupDownload ->
                        NcTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            title = if (index == 0) stringResource(id = R.string.nc_backup_download) else stringResource(
                                id = R.string.nc_backup_download_optional,
                                index + 1
                            ),
                            value = backupDownload,
                            onValueChange = {
                                onBackupDownloadTextChange(it, index)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceClaimInputScreenPreview() {
    InheritanceClaimInputContent(

    )
}