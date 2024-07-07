package com.nunchuk.android.signer.portal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface PortalAction : Parcelable

@Parcelize
data object AddNewPortal : PortalAction

@Parcelize
data class SetupPortal(val mnemonic: String, val numberOfWords: Int, val pin: String) :
    PortalAction

interface PortalActionWithPin : PortalAction

@Parcelize
data object GetXpub : PortalActionWithPin