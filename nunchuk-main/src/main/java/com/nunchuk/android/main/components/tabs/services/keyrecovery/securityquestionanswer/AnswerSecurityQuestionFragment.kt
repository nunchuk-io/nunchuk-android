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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnswerSecurityQuestionFragment : Fragment() {

    private val viewModel: AnswerSecurityQuestionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AnswerSecurityQuestionScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AnswerSecurityQuestionEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is AnswerSecurityQuestionEvent.ProcessFailure -> {
                    showError(message = event.message)
                }
                is AnswerSecurityQuestionEvent.OnVerifySuccess -> {
                    requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(
                            GlobalResultKey.SIGNATURE_EXTRA,
                            HashMap<String, String>()
                        )
                        putExtra(GlobalResultKey.SECURITY_QUESTION_TOKEN, event.token)
                        putExtra(GlobalResultKey.SECURITY_QUESTION_EXTRA_INFO, HashMap<String, String>().apply {
                            put(QUESTION_ID, event.questionId)
                            put(QUESTION_ANSWER, event.answer)
                        })
                    })
                    requireActivity().finish()
                }
            }
        }
    }

    companion object {
        const val QUESTION_ID = "question_id"
        const val QUESTION_ANSWER = "question_answer"
    }
}

@Composable
fun AnswerSecurityQuestionScreen(
    viewModel: AnswerSecurityQuestionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnswerSecurityQuestionScreenContent(answer = state.answer,
        question = state.question?.question.orEmpty(),
        error = state.error,
        onContinueClick = {
            viewModel.onContinueClicked()
        }, onTextChange = {
            viewModel.onAnswerTextChange(it)
        })
}

@Composable
fun AnswerSecurityQuestionScreenContent(
    answer: String = "",
    question: String = "",
    error: String = "",
    onContinueClick: () -> Unit = {},
    onTextChange: (value: String) -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                NcTopAppBar(title = "")
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_answer_security_question),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_answer_security_question_desc),
                    style = NunchukTheme.typography.body
                )
                if (question.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
                        text = question,
                        style = NunchukTheme.typography.title
                    )

                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(horizontal = 16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        title = stringResource(id = R.string.nc_answer),
                        value = answer,
                        error = error,
                        onValueChange = onTextChange
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = answer.isNotEmpty(),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun AnswerSecurityQuestionScreenContentPreview() {
    AnswerSecurityQuestionScreenContent()
}