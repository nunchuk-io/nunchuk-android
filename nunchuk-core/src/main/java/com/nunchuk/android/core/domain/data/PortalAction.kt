package com.nunchuk.android.core.domain.data

import android.net.Uri
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

@Parcelize
data object ImportWallet : PortalActionWithPin

@Parcelize
data class ExportWallet(val walletId: String) : PortalActionWithPin

@Parcelize
data object CheckFirmwareVersion : PortalAction

@Parcelize
data class UpdateFirmware(val uri: Uri) : PortalActionWithPin

@Parcelize
data class SignTransaction(val fingerPrint: String, val psbt: String) : PortalAction

@Parcelize
data class VerifyAddress(val address: String, val index: Int) : PortalActionWithPin