package com.nunchuk.android.core.domain.message

import com.nunchuk.android.core.domain.membership.GetServerWalletsUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.util.getGroupId
import com.nunchuk.android.messages.util.getLastMessageContentSafe
import com.nunchuk.android.messages.util.getNewWalletId
import com.nunchuk.android.messages.util.getTransactionId
import com.nunchuk.android.messages.util.getWalletId
import com.nunchuk.android.messages.util.getXfp
import com.nunchuk.android.messages.util.isAddKeyCompleted
import com.nunchuk.android.messages.util.isCoinControlUpdated
import com.nunchuk.android.messages.util.isDraftWalletResetEvent
import com.nunchuk.android.messages.util.isDraftWalletTimelockSetEvent
import com.nunchuk.android.messages.util.isGroupEmergencyLockdownStarted
import com.nunchuk.android.messages.util.isGroupMembershipRequestCreatedEvent
import com.nunchuk.android.messages.util.isGroupNameChanged
import com.nunchuk.android.messages.util.isGroupWalletCreatedEvent
import com.nunchuk.android.messages.util.isGroupWalletPrimaryOwnerUpdated
import com.nunchuk.android.messages.util.isInheritanceEvent
import com.nunchuk.android.messages.util.isKeyNameChanged
import com.nunchuk.android.messages.util.isRemoveAlias
import com.nunchuk.android.messages.util.isReplaceKeyChangeEvent
import com.nunchuk.android.messages.util.isServerTransactionEvent
import com.nunchuk.android.messages.util.isSetAlias
import com.nunchuk.android.messages.util.isTransactionCancelled
import com.nunchuk.android.messages.util.isTransactionHandleErrorMessageEvent
import com.nunchuk.android.messages.util.isTransactionReplaced
import com.nunchuk.android.messages.util.isTransferFundCompleted
import com.nunchuk.android.messages.util.isWalletCreated
import com.nunchuk.android.messages.util.isWalletInheritanceCanceled
import com.nunchuk.android.messages.util.isWalletReplacedEvent
import com.nunchuk.android.usecase.IsHandledEventUseCase
import com.nunchuk.android.usecase.SaveHandledEventUseCase
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.byzantine.DeleteKeyInWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.coin.SyncCoinControlData
import com.nunchuk.android.usecase.wallet.GetServerWalletUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class HandlePushMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val pushEventManager: PushEventManager,
    private val isHandledEventUseCase: IsHandledEventUseCase,
    private val saveHandledEventUseCase: SaveHandledEventUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val getServerWalletsUseCase: GetServerWalletsUseCase,
    private val syncCoinControlData: SyncCoinControlData,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val deleteKeyInWalletUseCase: DeleteKeyInWalletUseCase,
) : UseCase<TimelineEvent, Unit>(dispatcher) {
    override suspend fun execute(parameters: TimelineEvent) {
        when {
            parameters.isTransactionHandleErrorMessageEvent() || parameters.isServerTransactionEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    if (parameters.isTransactionHandleErrorMessageEvent()) {
                        pushEventManager.push(
                            PushEvent.MessageEvent(
                                parameters.getLastMessageContentSafe().orEmpty()
                            )
                        )
                    }
                    pushEventManager.push(
                        PushEvent.ServerTransactionEvent(
                            parameters.getWalletId().orEmpty(),
                            parameters.getTransactionId().orEmpty()
                        )
                    )
                }
            }

            parameters.isAddKeyCompleted() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(PushEvent.AddDesktopKeyCompleted)
                }
            }

            parameters.isWalletCreated() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.WalletCreate(
                            parameters.getWalletId().orEmpty(),
                            parameters.getGroupId().orEmpty(),
                        )
                    )
                }
            }

            parameters.isTransactionCancelled() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.TransactionCancelled(
                            parameters.getWalletId().orEmpty(),
                            parameters.getTransactionId().orEmpty()
                        )
                    )
                }
            }

            parameters.isDraftWalletResetEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.DraftResetWallet(
                            parameters.getGroupId().orEmpty()
                        )
                    )
                }
            }

            parameters.isDraftWalletTimelockSetEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.DraftWalletTimelockSet(
                            parameters.getGroupId().orEmpty()
                        )
                    )
                }
            }

            parameters.isGroupMembershipRequestCreatedEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.GroupMembershipRequestCreated(
                            parameters.getGroupId().orEmpty()
                        )
                    )
                }
            }

            parameters.isGroupWalletCreatedEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.GroupWalletCreated(
                            parameters.getWalletId().orEmpty()
                        )
                    )
                }
            }

            parameters.isGroupEmergencyLockdownStarted() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.GroupEmergencyLockdownStarted(
                            parameters.getWalletId().orEmpty()
                        )
                    )
                }
            }

            parameters.isInheritanceEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.InheritanceEvent(
                            walletId = parameters.getWalletId().orEmpty(),
                            isCancelled = parameters.isWalletInheritanceCanceled()
                        )
                    )
                }
            }

            parameters.isGroupNameChanged() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    val groupId = parameters.getGroupId().orEmpty()
                    val walletId = parameters.getWalletId().orEmpty()
                    if (groupId.isNotEmpty()) {
                        syncGroupWalletUseCase(groupId)
                        pushEventManager.push(PushEvent.WalletChanged(walletId))
                    } else {
                        getServerWalletUseCase(walletId)
                        pushEventManager.push(PushEvent.WalletChanged(walletId))
                    }
                }
            }

            parameters.isGroupWalletPrimaryOwnerUpdated() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.PrimaryOwnerUpdated(
                            parameters.getWalletId().orEmpty()
                        )
                    )
                }
            }

            parameters.isKeyNameChanged() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    syncGroupWalletsUseCase(Unit)
                    getServerWalletsUseCase(Unit)
                    pushEventManager.push(
                        PushEvent.SignedChanged(
                            parameters.getXfp().orEmpty()
                        )
                    )
                }
            }

            parameters.isTransactionReplaced() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)

                    pushEventManager.push(
                        PushEvent.ServerTransactionEvent(
                            parameters.getWalletId().orEmpty(),
                            parameters.getTransactionId().orEmpty()
                        )
                    )
                }
            }

            parameters.isSetAlias() || parameters.isRemoveAlias() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    val groupId = parameters.getGroupId().orEmpty()
                    val walletId = parameters.getWalletId().orEmpty()
                    if (groupId.isNotEmpty()) {
                        syncGroupWalletUseCase(groupId)
                    } else {
                        getServerWalletUseCase(walletId)
                    }
                }
            }

            parameters.isCoinControlUpdated() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)

                    val groupId = parameters.getGroupId().orEmpty()
                    val walletId = parameters.getWalletId().orEmpty()
                    if (walletId.isNotEmpty()) {
                        syncCoinControlData(
                            SyncCoinControlData.Param(
                                groupId,
                                walletId
                            )
                        )

                        pushEventManager.push(
                            PushEvent.CoinUpdated(
                                walletId
                            )
                        )
                    }
                }
            }

            parameters.isReplaceKeyChangeEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    pushEventManager.push(
                        PushEvent.ReplaceKeyChange(
                            parameters.getWalletId().orEmpty()
                        )
                    )
                }
            }

            parameters.isWalletReplacedEvent() -> {
                val result = isHandledEventUseCase.invoke(parameters.eventId)
                if (result.getOrDefault(false).not()) {
                    saveHandledEventUseCase.invoke(parameters.eventId)
                    val isTransferFundCompleted = parameters.isTransferFundCompleted()
                    val oldWalletId = parameters.getWalletId().orEmpty()
                    val oldGroupId = parameters.getGroupId().orEmpty()
                    val newWalletId = parameters.getNewWalletId().orEmpty()
                    if (oldGroupId.isEmpty()) {
                        getServerWalletsUseCase(Unit)
                    } else {
                        syncGroupWalletsUseCase(Unit)
                    }
                    if (newWalletId.isNotEmpty() && isTransferFundCompleted) {
                        getWalletDetail2UseCase(newWalletId).onSuccess {
                            pushEventManager.push(
                                PushEvent.WalletReplaced(
                                    newWalletId = newWalletId,
                                    newWalletName = it.name
                                )
                            )
                        }
                    }
                    deleteKeyInWalletUseCase(
                        DeleteKeyInWalletUseCase.Params(
                            walletId = oldWalletId,
                            groupId = oldGroupId
                        )
                    )
                }
            }
        }
    }
}