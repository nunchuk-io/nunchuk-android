package com.nunchuk.android.messages.components.detail.holder

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nunchuk.android.core.util.isCreated
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.components.detail.bindCanceledStatus
import com.nunchuk.android.messages.components.detail.bindWalletStatus
import com.nunchuk.android.messages.databinding.ItemWalletCardBinding
import com.nunchuk.android.model.toRoomWalletData

internal class NunchukWalletCardHolder(
    val binding: ItemWalletCardBinding,
    val denyWallet: () -> Unit,
    val cancelWallet: () -> Unit,
    val viewConfig: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val gson = Gson()

    fun bind(model: NunchukWalletMessage) {
        val roomWallet = model.roomWallet
        if (roomWallet?.jsonContent == null) {
            return
        }
        val body = roomWallet.jsonContent
        val initData = body.toRoomWalletData(gson)
        val ratio = "${initData.requireSigners} / ${initData.totalSigners}"
        val context = itemView.context
        binding.cancelWallet.text = context.getString(
            if (model.isOwner) R.string.nc_message_cancel_wallet else R.string.nc_message_deny_wallet
        )
        if (roomWallet.initEventId != model.timelineEvent.eventId) {
            binding.cancelWallet.isVisible = false
            binding.pendingKeys.isVisible = false
            binding.status.bindCanceledStatus()
            binding.viewConfig.setOnClickListener(null)
        } else {
            binding.cancelWallet.isVisible = !roomWallet.isCreated()
            binding.pendingKeys.isVisible = true
            binding.status.bindWalletStatus(roomWallet)
            val remainingKeys = initData.totalSigners - roomWallet.joinEventIds.size
            if (remainingKeys > 0) {
                binding.pendingKeys.text = context.getString(R.string.nc_message_pending_signers_to_assign, remainingKeys)
            } else {
                binding.pendingKeys.text = context.getString(R.string.nc_message_all_keys_assigned)
            }
            binding.viewConfig.setOnClickListener { viewConfig() }
        }
        CardHelper.adjustCardLayout(binding.root, binding.cardTopContainer, model.isOwner)
        binding.name.text = initData.name
        binding.configuration.text = context.getString(R.string.nc_message_creating_wallet, ratio)
        binding.cancelWallet.setOnClickListener { if (model.isOwner) cancelWallet() else denyWallet() }
    }

}

internal object CardHelper {
    fun adjustCardLayout(card: LinearLayout, top: View, isTopRight: Boolean) {
        if (isTopRight) {
            card.gravity = Gravity.END
            top.background = AppCompatResources.getDrawable(card.context, R.drawable.message_wallet_top_right_background)
        } else {
            card.gravity = Gravity.START
            top.background = AppCompatResources.getDrawable(card.context, R.drawable.message_wallet_top_left_background)
        }
    }
}

