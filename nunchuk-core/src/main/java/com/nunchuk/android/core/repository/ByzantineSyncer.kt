package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.api.TRANSACTION_PAGE_COUNT
import com.nunchuk.android.core.data.model.byzantine.GroupResponse
import com.nunchuk.android.core.data.model.byzantine.KeyHealthStatusDto
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.toAlert
import com.nunchuk.android.core.mapper.toByzantineGroup
import com.nunchuk.android.core.mapper.toGroupChatEntity
import com.nunchuk.android.core.mapper.toGroupEntity
import com.nunchuk.android.core.mapper.toKeyHealthStatus
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.GroupStatus
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.persistence.dao.AlertDao
import com.nunchuk.android.persistence.dao.GroupChatDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.KeyHealthStatusDao
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.persistence.entity.GroupChatEntity
import com.nunchuk.android.persistence.entity.KeyHealthStatusEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class ByzantineSyncer @Inject constructor(
    private val alertDao: AlertDao,
    private val groupDao: GroupDao,
    private val groupChatDao: GroupChatDao,
    private val keyHealthStatusDao: KeyHealthStatusDao,
    private val userWalletApiManager: UserWalletApiManager,
    private val accountManager: AccountManager,
    ncDataStore: NcDataStore,
    private val gson: Gson,
    applicationScope: CoroutineScope,
) {

    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    suspend fun syncAlerts(
        groupId: String
    ): List<Alert>? {
        val remoteList = arrayListOf<Alert>()
        var index = 0
        runCatching {
            while (true) {
                val response =
                    userWalletApiManager.groupWalletApi.getAlerts(groupId, offset = index)
                if (response.isSuccess.not()) return null
                val alertList = response.data.alerts.orEmpty().map { it.toAlert() }
                remoteList.addAll(alertList)
                if (response.data.alerts.orEmpty().size < TRANSACTION_PAGE_COUNT) break
                index += TRANSACTION_PAGE_COUNT
            }
        }.onFailure {
            return null
        }

        val updateOrInsertList = mutableListOf<AlertEntity>()

        val localMap = alertDao.getAlerts(groupId, getChatId(), chain.value).firstOrNull().orEmpty()
            .associateByTo(mutableMapOf()) { it.id }

        remoteList.forEach { remote ->
            val local = localMap[remote.id]
            if (local != null) {
                updateOrInsertList += local.copy(
                    viewable = remote.viewable,
                    payload = gson.toJson(remote.payload),
                    body = remote.body,
                    createdTimeMillis = remote.createdTimeMillis,
                    status = remote.status,
                    title = remote.title,
                    type = remote.type.name
                )
                localMap.remove(remote.id)
            } else {
                updateOrInsertList += remote.toAlertEntity(groupId)
            }
        }

        val deleteList = localMap.values.toList()

        if (updateOrInsertList.isNotEmpty() || deleteList.isNotEmpty()) {
            alertDao.updateData(updateOrInsertList, deleteList)
        }
        return updateOrInsertList.map { it.toAlert() }
    }

    suspend fun syncGroups(groups: List<GroupResponse> = emptyList()): List<ByzantineGroup>? {
        runCatching {
            val finalGroups =
                groups.ifEmpty { userWalletApiManager.groupWalletApi.getGroups().data.groups.orEmpty() }
            val groupLocals =
                groupDao.getGroups(accountManager.getAccount().chatId, chain.value).firstOrNull()
                    ?: emptyList()
            val allGroupIds = groupLocals.map { it.groupId }.toHashSet()
            val addGroupIds = HashSet<String>()
            val chatId = accountManager.getAccount().chatId
            groupDao.updateOrInsert(finalGroups.filter {
                it.status != GroupStatus.DELETED.name && it.id.isNullOrEmpty().not()
            }.map { group ->
                addGroupIds.add(group.id.orEmpty())
                group.toGroupEntity(chatId, chain.value, groupDao)
            }.toList())
            allGroupIds.removeAll(addGroupIds)
            if (allGroupIds.isNotEmpty()) {
                groupDao.deleteGroups(allGroupIds.toList(), chatId = chatId)
            }
            return finalGroups.map { it.toByzantineGroup() }
        }.onFailure { return null }
        return null
    }

    suspend fun syncGroup(groupId: String): ByzantineGroup? {
        runCatching {
            val response = userWalletApiManager.groupWalletApi.getGroup(groupId)
            if (response.isSuccess.not()) return null
            val groupRemote = response.data.data ?: return null
            if (groupRemote.status == GroupStatus.DELETED.name) {
                groupDao.deleteGroups(listOf(groupId), chatId = getChatId())
                return null
            }
            groupDao.updateOrInsert(groupRemote.toGroupEntity(getChatId(), chain.value, groupDao))
            return groupRemote.toByzantineGroup()
        }.onFailure { return null }
        return null
    }

    suspend fun syncKeyHealthStatus(groupId: String, walletId: String): List<KeyHealthStatus>? {
        runCatching {
            val localMap = keyHealthStatusDao.getKeys(groupId, walletId, getChatId(), chain.value)
                .firstOrNull().orEmpty().associateByTo(mutableMapOf()) { it.xfp }
            val response =
                userWalletApiManager.groupWalletApi.getWalletHealthStatus(groupId, walletId)
            val remoteList = arrayListOf<KeyHealthStatusDto>()
            remoteList.addAll(response.data.statuses)
            val updateOrInsertList = mutableListOf<KeyHealthStatusEntity>()

            remoteList.forEach { remote ->
                val local = localMap[remote.xfp]
                if (local != null) {
                    updateOrInsertList += local.copy(
                        canRequestHealthCheck = remote.canRequestHealthCheck,
                        lastHealthCheckTimeMillis = remote.lastHealthCheckTimeMillis ?: 0L,
                        xfp = remote.xfp,
                    )
                    localMap.remove(remote.xfp)
                } else {
                    updateOrInsertList += remote.toKeyHealthStatusEntity(groupId, walletId)
                }
            }

            val deleteList = localMap.values.toList()

            if (updateOrInsertList.isNotEmpty() || deleteList.isNotEmpty()) {
                keyHealthStatusDao.updateData(updateOrInsertList, deleteList)
            }
            return updateOrInsertList.map { it.toKeyHealthStatus() }
        }.onFailure { return null }
        return null
    }

    suspend fun syncGroupChat() {
        runCatching {
            val response =
                userWalletApiManager.groupWalletApi.getGroupChats()
            if (response.isSuccess.not()) return
            val remoteList = response.data.chats.orEmpty()
            val localMap = groupChatDao.getGroupChats(getChatId(), chain.value)
                .associateByTo(mutableMapOf()) { it.groupId }
            val updateOrInsertList = mutableListOf<GroupChatEntity>()
            remoteList.forEach { remote ->
                val local = localMap[remote.groupId]
                if (local != null) {
                    updateOrInsertList += local.copy(
                        createdTimeMillis = remote.createdTimeMillis ?: 0L,
                        historyPeriod = gson.toJson(remote.historyPeriod),
                        roomId = remote.roomId.orEmpty()
                    )
                    localMap.remove(remote.groupId)
                } else {
                    updateOrInsertList += remote.toGroupChatEntity(getChatId(), chain.value.name)
                }
            }
            val deleteList = localMap.values.toList()
            if (updateOrInsertList.isNotEmpty() || deleteList.isNotEmpty()) {
                groupChatDao.updateData(updateOrInsertList, deleteList)
            }
        }
    }

    private fun Alert.toAlertEntity(
        groupId: String
    ): AlertEntity {
        return AlertEntity(
            id = id,
            viewable = viewable,
            body = body,
            createdTimeMillis = createdTimeMillis,
            status = status,
            title = title,
            chatId = getChatId(),
            type = type.name,
            chain = chain.value,
            payload = gson.toJson(payload),
            groupId = groupId
        )
    }

    private fun KeyHealthStatusDto.toKeyHealthStatusEntity(
        groupId: String,
        walletId: String
    ): KeyHealthStatusEntity {
        return KeyHealthStatusEntity(
            xfp = xfp,
            canRequestHealthCheck = canRequestHealthCheck,
            lastHealthCheckTimeMillis = lastHealthCheckTimeMillis ?: 0L,
            chatId = getChatId(),
            chain = chain.value,
            groupId = groupId,
            walletId = walletId
        )
    }

    private fun getChatId(): String {
        return accountManager.getAccount().chatId
    }
}