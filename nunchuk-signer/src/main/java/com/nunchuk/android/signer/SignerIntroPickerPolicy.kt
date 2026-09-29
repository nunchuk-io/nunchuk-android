package com.nunchuk.android.signer

import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.core.util.toWalletTypeOrNull
import com.nunchuk.android.model.SupportedSignerConfig
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType

/** Which keys are visible/enabled, independently of how a selected device is opened. */
internal class SignerIntroPickerPolicy(private val request: SignerIntroRequest) {
    private val keyFlow = request.keyFlow
    private val walletType = request.walletType
    private val onChainAddSignerParam = request.deviceParams
    private val isAddInheritanceOffChainSetup = request.flow is SignerIntroFlow.OffChainInheritanceKey
    private val isOffChainClaim = (request.flow as? SignerIntroFlow.ClaimInheritance)?.isOnChain == false
    private val isInheritanceSlot = when (val flow = request.flow) {
        is SignerIntroFlow.OnChainTimelockKey -> flow.isInheritanceKey
        is SignerIntroFlow.OffChainInheritanceKey, is SignerIntroFlow.ClaimInheritance,
        is SignerIntroFlow.VerifyBackup -> true
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey -> false
    }

    /**
     * Owner setup and the off-chain claim draw their cards from the server's inheritance-key list
     * for [walletType]; the static catalog only stands in until that list is available.
     */
    private val isServerDrivenInheritancePicker = isAddInheritanceOffChainSetup || isOffChainClaim

    val usesWalletConfigs = onChainAddSignerParam != null
    val loadsExistingSigners = onChainAddSignerParam != null
    val initialSigners: List<SupportedSigner> = when {
        isOffChainClaim -> offChainInheritanceClaimKeyTypes
        isAddInheritanceOffChainSetup -> offChainInheritanceSetupKeyTypes
        else -> request.supportedSigners
    }

    fun applyConfigs(state: SignerIntroState, configs: List<SupportedSignerConfig>): SignerIntroState {
        val relevant = filterConfigsByWalletType(configs, walletType)
        return state.copy(
            // A server-driven picker keeps its fallback until a matching inheritance list arrives.
            supportedSigners = if (isServerDrivenInheritancePicker) state.supportedSigners
                else convertToSupportedSigners(relevant),
            eligibleSupportedSigners = convertToSupportedSigners(
                relevant.filter { it.isInheritanceKey == isInheritanceSlot }
            ).withClaimSoftwareKey(),
        )
    }

    /**
     * The Beneficiary may hold nothing but the seed phrase, so the claim keeps the software key
     * even though an inheritance-key list (made for the owner's setup) never names one.
     */
    private fun List<SupportedSigner>.withClaimSoftwareKey(): List<SupportedSigner> =
        if (isOffChainClaim && isNotEmpty() && none { it.type == SignerType.SOFTWARE }) {
            this + offChainInheritanceClaimKeyTypes.filter { it.type == SignerType.SOFTWARE }
        } else {
            this
        }

    private fun calculateIsGenericAirgapEnable(
        state: SignerIntroState,
    ): Boolean {
        val supportedSigners = state.supportedSigners
        val isDisableAll = keyFlow != KeyFlow.NONE
        // The BYOH picker is server-driven, so this row follows the server's inheritance-key list
        // rather than the static fallback the state was seeded with. Until that list arrives the
        // cards are drawn from the fallback, so this row reads it too: disabling on an empty list
        // would leave Generic Airgap as the only greyed-out entry whenever the configs are not in
        // yet (cold cache, a cache written by a build the server gated out, a failed refresh).
        val signers = if (isServerDrivenInheritancePicker) {
            state.eligibleSupportedSigners.ifEmpty { supportedSigners }
        } else {
            supportedSigners
        }
        return (signers.isEmpty()
                || signers.any { it.type == SignerType.AIRGAP && it.tag == null }) && isDisableAll.not()
    }

    fun displayInfos(currentState: SignerIntroState): List<SignerDisplayInfo> {
        val (signersToDisplay, allowedSigners) = resolveSignersToDisplay(currentState)
        val isDisableAll = keyFlow != KeyFlow.NONE

        return signersToDisplay.mapNotNull { signer ->
            signer.toDisplayInfo()?.copy(
                isDisabled = signer.isDisabledIn(
                    allowedSigners = allowedSigners,
                    isDisableAll = isDisableAll,
                    onChainAddSignerParam = onChainAddSignerParam,
                    keyFlow = keyFlow,
                )
            )
        } + SignerDisplayInfo(
            iconRes = R.drawable.ic_split,
            titleRes = R.string.nc_generic_airgap,
            keyType = KeyType.GENERIC_AIRGAP,
            category = SignerDisplayCategory.ROW_SIMPLE,
            isDisabled = !calculateIsGenericAirgapEnable(currentState),
        )

    }

    private fun resolveSignersToDisplay(
        state: SignerIntroState,
    ): Pair<List<SupportedSigner>, List<SupportedSigner>> = when {
        state.eligibleSupportedSigners.isNotEmpty() && onChainAddSignerParam != null -> {
            state.eligibleSupportedSigners.inPickerOrder() to state.eligibleSupportedSigners
        }
        onChainAddSignerParam != null && state.supportedSigners.isNotEmpty() -> {
            state.supportedSigners.inPickerOrder() to state.supportedSigners
        }
        state.supportedSigners.isNotEmpty() -> {
            val allowedSigners = state.supportedSigners.filter {
                !(it.type == SignerType.AIRGAP && it.tag == null)
            }
            mergeWithDefaultSigners(state.supportedSigners) to allowedSigners
        }
        else -> {
            defaultSupportedSigners to emptyList()
        }
    }

    /**
     * The BYOH and claim pickers' card order is fixed by the design, so neither the server's
     * ordering of supported_signers nor the order the fallback list is written in may decide it.
     * Anything the design does not name keeps its relative position at the end. Every other flow
     * is left in the order it was given.
     */
    private fun List<SupportedSigner>.inPickerOrder(): List<SupportedSigner> {
        val order = when {
            isAddInheritanceOffChainSetup -> offChainInheritanceCardOrder
            isOffChainClaim -> offChainInheritanceClaimCardOrder
            else -> return this
        }
        return sortedBy { signer -> order.indexOf(signer.toKeyType()).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    private fun mergeWithDefaultSigners(
        supportedSigners: List<SupportedSigner>,
    ): List<SupportedSigner> {
        val result = defaultSupportedSigners.toMutableList()
        supportedSigners.forEach { signer ->
            if (signer.type == SignerType.AIRGAP && signer.tag == null) return@forEach
            if (result.none { it.type == signer.type && it.tag == signer.tag }) {
                result.add(signer)
            }
        }
        return result
    }

    /**
     * Keeps only the signer configs that match the wallet type this flow is building ([walletType]).
     * The backend may return signers scoped to unrelated wallet types (e.g. a SOFTWARE signer for
     * LIQUID), which must not be offered when building, say, a MINISCRIPT wallet. When no wallet
     * type is provided, all configs are kept so flows that don't scope by wallet type are unaffected.
     */
    private fun filterConfigsByWalletType(
        configs: List<SupportedSignerConfig>,
        walletType: WalletType?,
    ): List<SupportedSignerConfig> {
        if (walletType == null) return configs
        return configs.filter { it.walletType == walletType.name }
    }

    private fun convertToSupportedSigners(configs: List<SupportedSignerConfig>): List<SupportedSigner> {
        return configs.mapNotNull { config ->
            val signerType = runCatching { SignerType.valueOf(config.signerType) }.getOrNull()
            val signerTag = config.signerTag
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { SignerTag.valueOf(it) }.getOrNull() }
            val walletType = config.walletType.toWalletTypeOrNull()

            if (signerType == null) {
                return@mapNotNull null
            }

            SupportedSigner(
                type = signerType,
                tag = signerTag,
                walletType = walletType,
                addressType = AddressType.NATIVE_SEGWIT
            )
        }
    }

}
