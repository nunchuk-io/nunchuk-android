package com.nunchuk.android.share

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
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
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder
) : GetCurrentUserAsContactUseCase {

    override fun execute(): Flow<Contact?> = flow {
        emit(getCurrentAsContact())
    }

    private fun getCurrentAsContact() =
        if (signInModeHolder.getCurrentMode().isGuestMode()) null else accountManager.getAccount()
            .toContact()

}

private fun AccountInfo.toContact(): Contact = Contact(
    id = chatId,
    name = name,
    email = email,
    gender = "",
    avatar = avatarUrl.orEmpty(),
    status = "",
    chatId = chatId,
    loginType = getLoginType(loginType),
    username = username
)

private fun getLoginType(loginType: Int): String {
    if (loginType == SignInMode.PRIMARY_KEY.value) return Contact.PRIMARY_KEY
    if (loginType == SignInMode.EMAIL.value) return Contact.EMAIL
    return Contact.UNKNOWN
}
