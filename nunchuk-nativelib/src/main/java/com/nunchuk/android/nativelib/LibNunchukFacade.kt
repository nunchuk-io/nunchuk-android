package com.nunchuk.android.nativelib

import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.bridge.toBridge
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.ExportFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LibNunchukFacade @Inject constructor(
    private val nunchukAndroid: LibNunchukAndroid
) {

    @Throws(NCNativeException::class)
    fun initNunchuk(appSettings: AppSettings) {
        nunchukAndroid.initNunchuk(
            chain = appSettings.chain.ordinal,
            hwiPath = appSettings.hwiPath,
            enableProxy = appSettings.enableProxy,
            testnetServers = appSettings.testnetServers,
            backendType = appSettings.backendType.ordinal,
            storagePath = appSettings.storagePath
        )
    }

    @Throws(NCNativeException::class)
    fun createSigner(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ) = nunchukAndroid.createSigner(
        name = name,
        xpub = xpub,
        publicKey = publicKey,
        derivationPath = derivationPath,
        masterFingerprint = masterFingerprint
    )

    @Throws(NCNativeException::class)
    fun getRemoteSigner() = nunchukAndroid.getRemoteSigner()

    @Throws(NCNativeException::class)
    fun getRemoteSigners(): List<SingleSigner> = nunchukAndroid.getRemoteSigners()

    @Throws(NCNativeException::class)
    fun updateSigner(signer: SingleSigner) {
        nunchukAndroid.updateSigner(signer)
    }

    @Throws(NCNativeException::class)
    fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String) {
        nunchukAndroid.deleteRemoteSigner(masterFingerprint, derivationPath)
    }

    @Throws(NCNativeException::class)
    fun createWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = nunchukAndroid.createWallet(
        name = name,
        totalRequireSigns = totalRequireSigns,
        signers = signers,
        addressType = addressType.ordinal,
        isEscrow = isEscrow,
        description = description
    )

    @Throws(NCNativeException::class)
    fun draftWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = nunchukAndroid.draftWallet(
        name = name,
        totalRequireSigns = totalRequireSigns,
        signers = signers,
        addressType = addressType.ordinal,
        isEscrow = isEscrow,
        description = description
    )

    @Throws(NCNativeException::class)
    fun getWallets() = nunchukAndroid.getWallets()

    @Throws(NCNativeException::class)
    fun exportWallet(walletId: String, filePath: String, format: ExportFormat) = nunchukAndroid.exportWallet(
        walletId = walletId,
        filePath = filePath,
        format = format.ordinal
    )

    @Throws(NCNativeException::class)
    fun exportCoboWallet(walletId: String) = nunchukAndroid.exportCoboWallet(
        walletId = walletId
    )

    @Throws(NCNativeException::class)
    fun getWallet(walletId: String) = nunchukAndroid.getWallet(walletId)

    @Throws(NCNativeException::class)
    fun updateWallet(wallet: Wallet) = nunchukAndroid.updateWallet(wallet.toBridge())

    //https://iancoleman.io/bip39/
    @Throws(NCNativeException::class)
    fun generateMnemonicCode() = "member gift grocery injury plunge track such sphere symptom liar asthma neck route hair promote motion public pupil nuclear flat afraid pen lady frown"
}