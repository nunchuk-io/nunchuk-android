package com.nunchuk.android.nativelib

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SingleSigner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LibNunchukFacade @Inject constructor(
    private val nunchukAndroid: LibNunchukAndroid
) {

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

    fun getRemoteSigner() = nunchukAndroid.getRemoteSigner()

    fun getRemoteSigners(): List<SingleSigner> = nunchukAndroid.getRemoteSigners(ArrayList())

    fun updateSigner(signer: SingleSigner) {
        nunchukAndroid.updateSigner(signer)
    }

    fun getWallets() = nunchukAndroid.getWallets(ArrayList())

    fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String) {
        nunchukAndroid.deleteRemoteSigner(masterFingerprint, derivationPath)
    }

}