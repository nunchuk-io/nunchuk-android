/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.membership.model.SecurityQuestionModel

class RecoveryQuestionBottomSheetFragment : BaseComposeBottomSheet() {

    private val args: RecoveryQuestionBottomSheetFragmentArgs by navArgs()
    private val viewModel by viewModels<RecoveryQuestionBottomSheetViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NunchukTheme {
                    RecoveryQuestionScreen(args, viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is RecoveryQuestionBottomSheetEvent.SelectQuestion -> setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putParcelable(
                            EXTRA_SELECTED_QUESTION,
                            event.question
                        )
                    })
            }
            dismissAllowingStateLoss()
        }
    }

    companion object {
        const val REQUEST_KEY = "RecoveryQuestionBottomSheetFragment"
        const val EXTRA_SELECTED_QUESTION = "EXTRA_SELECTED_QUESTION"
    }
}


@Composable
private fun RecoveryQuestionScreen(
    args: RecoveryQuestionBottomSheetFragmentArgs,
    viewModel: RecoveryQuestionBottomSheetViewModel,
) {
    RecoveryQuestionScreenContent(
        questions = args.questions.toList(),
        viewModel::onSelectQuestion
    )
}

@Composable
private fun RecoveryQuestionScreenContent(
    questions: List<SecurityQuestionModel> = emptyList(),
    onSelectQuestion: (question: SecurityQuestionModel) -> Unit = {},
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        )
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(questions) {
                Text(
                    text = it.question.orEmpty(),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectQuestion(it)
                        }
                )
            }
        }
    }
}

@Preview
@Composable
fun TapSignerListContentPreview() {
    NunchukTheme {
        RecoveryQuestionScreenContent(
            questions = listOf(
                SecurityQuestionModel(
                    question = "What is the name of your favorite pet?"
                ),
                SecurityQuestionModel(
                    question = "What is your mother's maiden name?"
                ),
            )
        )
    }
}
