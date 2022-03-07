package com.nunchuk.android.share

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetCurrentUserAsContactUseCase {
    fun execute(): Flow<Contact?>
}

internal class GetCurrentUserAsContactUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager
) : GetCurrentUserAsContactUseCase {

    override fun execute(): Flow<Contact?> = flow {
        emit(getCurrentAsContact())
    }

    private fun getCurrentAsContact() = if (SignInModeHolder.currentMode.isGuestMode()) null else accountManager.getAccount().toContact()

}

private fun AccountInfo.toContact(): Contact = Contact(
    id = chatId,
    name = name,
    email = email,
    gender = "",
    avatar = avatarUrl.orEmpty(),
    status = "",
    chatId = chatId
)
