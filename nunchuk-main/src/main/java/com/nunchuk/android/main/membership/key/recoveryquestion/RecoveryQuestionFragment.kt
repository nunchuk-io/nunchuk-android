package com.nunchuk.android.main.membership.key.recoveryquestion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable

class RecoveryQuestionFragment : MembershipFragment() {
    private val viewModel: RecoveryQuestionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RecoveryQuestionScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when(it) {
                RecoveryQuestionEvent.ContinueStepEvent -> handleContinue()
                is RecoveryQuestionEvent.GetSecurityQuestionSuccess -> handleShowSecurityQuestion(it)
                is RecoveryQuestionEvent.Loading -> showOrHideLoading(it.isLoading)
                is RecoveryQuestionEvent.ShowError -> showError(it.message)
                RecoveryQuestionEvent.ConfigRecoveryQuestionSuccess -> findNavController().popBackStack()
            }
        }
    }

    private fun handleShowSecurityQuestion(event: RecoveryQuestionEvent.GetSecurityQuestionSuccess) {
        findNavController().navigate(
            RecoveryQuestionFragmentDirections.actionRecoveryQuestionFragmentToRecoveryQuestionBottomSheetFragment(
                event.questions.toMutableList().apply {
                    add(SecurityQuestionModel(SecurityQuestionModel.CUSTOM_QUESTION_ID, getString(R.string.nc_create_my_own_question)))
                }.toTypedArray()
            )
        )
    }

    private fun handleContinue() {
        setFragmentResult(REQUEST_KEY, Bundle())
        findNavController().popBackStack(R.id.addKeyListFragment, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(RecoveryQuestionBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val question =
                bundle.parcelable<SecurityQuestionModel>(RecoveryQuestionBottomSheetFragment.EXTRA_SELECTED_QUESTION)
                    ?: return@setFragmentResultListener
            viewModel.updateQuestion(question)
        }
    }

    companion object {
        const val REQUEST_KEY = "RecoveryQuestionFragment"
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun RecoveryQuestionScreen(
    viewModel: RecoveryQuestionViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecoveryQuestionScreenContent(state,
        remainTime,
        viewModel::onContinueClicked,
        onQuestionClicked = {
            viewModel.getSecurityQuestionList(it)
        },
        onInputAnswerTextChange = { index, value ->
            viewModel.updateAnswer(index, value)
        },
        onInputCustomQuestionTextChange = { index, value ->
            viewModel.updateCustomQuestion(index, value)
        })
}

@Composable
fun RecoveryQuestionScreenContent(
    state: RecoveryQuestionState = RecoveryQuestionState.Empty,
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onQuestionClicked: (index: Int) -> Unit = {},
    onInputAnswerTextChange: (index: Int, value: String) -> Unit = { _, _ -> },
    onInputCustomQuestionTextChange: (index: Int, value: String) -> Unit = { _, _ -> },
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(stringResource(R.string.nc_estimate_remain_time, remainTime))

                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                            text = stringResource(R.string.nc_setup_key_recovery),
                            style = NunchukTheme.typography.heading
                        )
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            text = stringResource(id = R.string.nc_setup_key_recovery_desc),
                            style = NunchukTheme.typography.body
                        )
                    }
                    items(state.recoveries) { recoverQuestion ->
                        QuestionRow(index = recoverQuestion.index,
                            question = recoverQuestion.question,
                            answer = recoverQuestion.answer,
                            onQuestionClicked = onQuestionClicked,
                            onInputAnswerTextChange = { value ->
                                onInputAnswerTextChange(recoverQuestion.index, value)
                            },
                            onInputCustomQuestionTextChange = { value ->
                                onInputCustomQuestionTextChange(recoverQuestion.index, value)
                            })
                    }
                }

                NcPrimaryDarkButton(
                    enabled = state.recoveries.all { it.answer.isNotEmpty() },
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
fun QuestionRow(
    index: Int = 0,
    question: SecurityQuestionModel = SecurityQuestionModel(question = "Question"),
    answer: String = "Value Answer",
    onQuestionClicked: (index: Int) -> Unit = {},
    onInputAnswerTextChange: (value: String) -> Unit = {},
    onInputCustomQuestionTextChange: (value: String) -> Unit = {},
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        ConstraintLayout {
            val (title, input) = createRefs()
            val questionTitle = stringResource(R.string.nc_question_data, index.inc())
            Text(text = questionTitle,
                style = NunchukTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .constrainAs(title) {})

            Row(
                modifier = Modifier
                    .border(
                        width = 1.dp, color = NcColor.border, shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = { onQuestionClicked(index) })
                    .constrainAs(input) {
                        top.linkTo(title.bottom)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val textStyle =
                    if (question.question.isNullOrEmpty()) NunchukTheme.typography.body.copy(color = NcColor.boulder) else NunchukTheme.typography.body
                Text(
                    text = question.question.orEmpty().ifBlank {stringResource(id = R.string.nc_select_a_question)  },
                    style = textStyle,
                    modifier = Modifier
                        .padding(top = 14.dp, start = 12.dp, bottom = 14.dp)
                        .weight(1f)
                        .defaultMinSize(minWidth = TextFieldDefaults.MinWidth)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_expand),
                    modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                    contentDescription = null,
                )
            }
        }
        if (question.id == SecurityQuestionModel.CUSTOM_QUESTION_ID) {
            NcTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                title = "",
                value = question.customQuestion.orEmpty(),
                onValueChange = onInputCustomQuestionTextChange
            )
        }
        NcTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            title = stringResource(id = R.string.nc_answer),
            value = answer,
            enabled = question.isValidQuestion,
            onValueChange = onInputAnswerTextChange
        )

        Divider(modifier = Modifier.padding(top = 16.dp), thickness = 1.dp, color = NcColor.whisper)
    }
}

@Preview
@Composable
private fun RecoveryQuestionScreenContentPreview() {
    RecoveryQuestionScreenContent()
}
