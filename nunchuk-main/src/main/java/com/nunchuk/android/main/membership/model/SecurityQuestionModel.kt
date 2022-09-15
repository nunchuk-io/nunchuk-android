package com.nunchuk.android.main.membership.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SecurityQuestionModel(
    val id: String = "",
    val question: String? = null,
    val customQuestion: String? = null
) : Parcelable {
    val isValidQuestion: Boolean
        get() = (id != CUSTOM_QUESTION_ID && question.isNullOrEmpty().not()) || (id == CUSTOM_QUESTION_ID && customQuestion.isNullOrEmpty().not())

    companion object {
        const val CUSTOM_QUESTION_ID = "custom_question_id"
    }
}