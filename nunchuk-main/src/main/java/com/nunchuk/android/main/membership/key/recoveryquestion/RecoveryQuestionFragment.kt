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

package com.nunchuk.android.main.membership.key.recoveryquestion

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import com.nunchuk.android.model.isByzantine
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryQuestionFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RecoveryQuestionViewModel by viewModels()
    private val args: RecoveryQuestionFragmentArgs by navArgs()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val signatureMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        ?: return@registerForActivityResult
                val securityQuestionToken =
                    data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                val confirmCodeMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.CONFIRM_CODE)
                        .orEmpty()
                viewModel.securityQuestionUpdate(
                    signatureMap,
                    securityQuestionToken,
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_TOKEN].orEmpty(),
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_NONCE].orEmpty()
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RecoveryQuestionScreen(viewModel, membershipStepManager, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is RecoveryQuestionEvent.ContinueStepEvent -> handleContinue()
                is RecoveryQuestionEvent.GetSecurityQuestionSuccess -> handleShowSecurityQuestion(it)
                is RecoveryQuestionEvent.Loading -> showOrHideLoading(it.isLoading)
                is RecoveryQuestionEvent.ShowError -> showError(it.message)
                is RecoveryQuestionEvent.ConfigRecoveryQuestionSuccess -> findNavController().popBackStack()
                is RecoveryQuestionEvent.CalculateRequiredSignaturesSuccess -> {
                    navigator.openWalletAuthentication(
                        walletId = it.walletId,
                        userData = it.userData,
                        requiredSignatures = it.requiredSignatures,
                        type = it.type,
                        action = TargetAction.UPDATE_SECURITY_QUESTIONS.name,
                        launcher = launcher,
                        activityContext = requireActivity()
                    )
                }
                RecoveryQuestionEvent.RecoveryQuestionUpdateSuccess -> {
                    val message = if (viewModel.state.value.plan.isByzantine()) {
                        getString(R.string.nc_security_questions_updated)
                    } else {
                        getString(R.string.nc_key_recovery_questions_updated)
                    }
                    NCToastMessage(requireActivity()).show(message = message)
                }
                RecoveryQuestionEvent.DiscardChangeClick -> findNavController().popBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateClearFocus(false)
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateClearFocus(true)
    }

    private fun handleShowSecurityQuestion(event: RecoveryQuestionEvent.GetSecurityQuestionSuccess) {
        findNavController().navigate(
            RecoveryQuestionFragmentDirections.actionRecoveryQuestionFragmentToRecoveryQuestionBottomSheetFragment(
                event.questions.toMutableList().apply {
                    add(
                        SecurityQuestionModel(
                            SecurityQuestionModel.CUSTOM_QUESTION_ID,
                            getString(R.string.nc_create_my_own_question)
                        )
                    )
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

@Composable
fun RecoveryQuestionScreen(
    viewModel: RecoveryQuestionViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    args: RecoveryQuestionFragmentArgs
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecoveryQuestionScreenContent(recoveries = state.recoveries,
        clearFocusRequest = state.clearFocusRequest,
        remainTime = remainTime,
        isRecoveryFlow = args.isRecoveryFlow,
        onContinueClicked = viewModel::onContinueClicked,
        onQuestionClicked = {
            viewModel.getSecurityQuestionList(it)
        },
        onDiscardChangeClicked = {
            viewModel.onDiscardChangeClick()
        },
        onInputAnswerTextChange = { index, value ->
            viewModel.updateAnswer(index, value)
        },
        onInputCustomQuestionTextChange = { index, value ->
            viewModel.updateCustomQuestion(index, value)
        },
        onFocusChange = { index, _ ->
            viewModel.updateMaskAnswer(index = index)
        })
}

@Composable
fun RecoveryQuestionScreenContent(
    recoveries: List<RecoveryData> = emptyList(),
    clearFocusRequest: Boolean = false,
    remainTime: Int = 0,
    isRecoveryFlow: Boolean = true,
    onContinueClicked: () -> Unit = {},
    onDiscardChangeClicked: () -> Unit = {},
    onQuestionClicked: (index: Int) -> Unit = {},
    onInputAnswerTextChange: (index: Int, value: String) -> Unit = { _, _ -> },
    onInputCustomQuestionTextChange: (index: Int, value: String) -> Unit = { _, _ -> },
    onFocusChange: (index: Int, focused: Boolean) -> Unit = { _, _ -> }
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val title = if (isRecoveryFlow.not()) {
                    stringResource(R.string.nc_estimate_remain_time, remainTime)
                } else {
                    ""
                }
                NcTopAppBar(title = title)

                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                            text = stringResource(R.string.nc_setup_security_questions),
                            style = NunchukTheme.typography.heading
                        )
                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            text = stringResource(id = R.string.nc_setup_security_questions_desc),
                            style = NunchukTheme.typography.body
                        )
                    }
                    items(recoveries) { recoverQuestion ->
                        QuestionRow(index = recoverQuestion.index,
                            question = recoverQuestion.question,
                            answer = recoverQuestion.answer,
                            isRecoveryFlow = isRecoveryFlow,
                            isShowMask = recoverQuestion.isShowMask,
                            onQuestionClicked = onQuestionClicked,
                            isRequestClearFocus = clearFocusRequest,
                            onInputAnswerTextChange = { value ->
                                onInputAnswerTextChange(recoverQuestion.index, value)
                            },
                            onInputCustomQuestionTextChange = { value ->
                                onInputCustomQuestionTextChange(recoverQuestion.index, value)
                            },
                            onFocusChange = {
                                onFocusChange(recoverQuestion.index, it)
                            })
                    }
                }
                val continueEnabled =
                    recoveries.all { it.answer.isNotEmpty() } || (recoveries.any { it.change } && isRecoveryFlow)
                NcPrimaryDarkButton(
                    enabled = continueEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = if (isRecoveryFlow) stringResource(id = R.string.nc_continue_save_changes) else stringResource(id = R.string.nc_text_continue))
                }
                if (isRecoveryFlow) {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .height(48.dp),
                        onClick = onDiscardChangeClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_discard_changes))
                    }
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
    isRecoveryFlow: Boolean = false,
    isShowMask: Boolean = false,
    isRequestClearFocus: Boolean = false,
    isShowError: Boolean = false,
    onQuestionClicked: (index: Int) -> Unit = {},
    onInputAnswerTextChange: (value: String) -> Unit = {},
    onInputCustomQuestionTextChange: (value: String) -> Unit = {},
    onFocusChange: (focused: Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    if (isRequestClearFocus) {
        focusManager.clearFocus()
    }
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
                    text = question.question.orEmpty()
                        .ifBlank { stringResource(id = R.string.nc_select_a_question) },
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
            val error = if (question.customQuestion.isNullOrBlank()) {
                stringResource(id = R.string.nc_please_write_your_question)
            } else ""
            NcTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                title = "",
                value = question.customQuestion.orEmpty(),
                onValueChange = onInputCustomQuestionTextChange,
                error = error,
                showErrorMessageOnly = true
            )
        }
        val vi =
            if (isRecoveryFlow && isShowMask) {
                MaskAnswerTransformation()
            } else {
                VisualTransformation.None
            }
        NcTextField(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            title = stringResource(id = R.string.nc_answer),
            value = answer,
            enabled = question.isValidQuestion,
            visualTransformation = vi,
            onValueChange = { onInputAnswerTextChange(it) },
            onFocusEvent = { focusState ->
                if (isRecoveryFlow && isShowMask && focusState.isFocused) {
                    onFocusChange(focusState.isFocused)
                }
            })

        Divider(modifier = Modifier.padding(top = 16.dp), thickness = 1.dp, color = NcColor.whisper)
    }
}

private class MaskAnswerTransformation(val mask: Char = '\u2022') : VisualTransformation {

    private val formattedText = mask.toString().repeat(8)

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return 8
        }

        override fun transformedToOriginal(offset: Int): Int {
            return 0
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {

        return TransformedText(
            text = AnnotatedString(formattedText),
            offsetMapping
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is androidx.compose.ui.text.input.PasswordVisualTransformation) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask.hashCode()
    }
}

@Preview
@Composable
private fun RecoveryQuestionScreenContentPreview() {
    RecoveryQuestionScreenContent()
}
