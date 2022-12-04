package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnswerSecurityQuestionFragment : Fragment() {

    private val viewModel: KeyRecoveryAnswerSecurityQuestionViewModel by viewModels()
    private val args: AnswerSecurityQuestionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AnswerSecurityQuestionScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AnswerSecurityQuestionEvent.ContinueClick -> {
                }
                is AnswerSecurityQuestionEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is AnswerSecurityQuestionEvent.DownloadBackupKeySuccess -> {
                    findNavController().navigate(
                        AnswerSecurityQuestionFragmentDirections.actionAnswerSecurityQuestionFragmentToBackupDownloadFragment(
                            signer = args.signer,
                            backupKey = event.backupKey
                        )
                    )
                }
                is AnswerSecurityQuestionEvent.ProcessFailure -> {
                    showError(message = event.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AnswerSecurityQuestionScreen(
    viewModel: KeyRecoveryAnswerSecurityQuestionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnswerSecurityQuestionScreenContent(answer = state.answer,
        question = state.question?.question.orEmpty(),
        error = state.error,
        onContinueClick = {
            viewModel.downloadBackupKey()
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
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
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

                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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