package com.nunchuk.android.nativelib

import com.nunchuk.android.exception.NunchukNativeException
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LibNunchukFacade @Inject constructor(
    private val nunchukAndroid: LibNunchukAndroid
) {

    @Throws(NunchukNativeException::class)
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

    @Throws(NunchukNativeException::class)
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

    @Throws(NunchukNativeException::class)
    fun getRemoteSigner() = nunchukAndroid.getRemoteSigner()

    @Throws(NunchukNativeException::class)
    fun getRemoteSigners(): List<SingleSigner> = nunchukAndroid.getRemoteSigners()

    @Throws(NunchukNativeException::class)
    fun updateSigner(signer: SingleSigner) {
        nunchukAndroid.updateSigner(signer)
    }

    @Throws(NunchukNativeException::class)
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

    @Throws(NunchukNativeException::class)
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

    @Throws(NunchukNativeException::class)
    fun getWallets() = nunchukAndroid.getWallets()

    @Throws(NunchukNativeException::class)
    fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String) {
        nunchukAndroid.deleteRemoteSigner(masterFingerprint, derivationPath)
    }

}