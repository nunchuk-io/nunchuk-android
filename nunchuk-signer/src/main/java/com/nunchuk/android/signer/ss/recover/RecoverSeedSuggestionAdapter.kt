package com.nunchuk.android.signer.ss.recover

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.BaseViewHolder
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ItemRecoverSeedSuggestionBinding
import com.nunchuk.android.widget.util.inflate

internal class RecoverSeedSuggestionAdapter(
    private val onItemSelectedListener: (String) -> Unit
) : RecyclerView.Adapter<ConfirmSeedViewHolder>() {

    internal var items: List<String> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfirmSeedViewHolder(
        parent.inflate(R.layout.item_recover_seed_suggestion),
        onItemSelectedListener
    )

    override fun onBindViewHolder(holder: ConfirmSeedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class ConfirmSeedViewHolder(
    itemView: View,
    val onItemSelectedListener: (String) -> Unit
) : BaseViewHolder<String>(itemView) {

    private val binding = ItemRecoverSeedSuggestionBinding.bind(itemView)

    override fun bind(data: String) {
        binding.word.text = data
        binding.root.setOnClickListener { onItemSelectedListener(data) }
    }

}
