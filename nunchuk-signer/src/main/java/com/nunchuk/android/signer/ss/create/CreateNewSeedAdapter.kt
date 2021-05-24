package com.nunchuk.android.signer.ss.create

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ItemNewSeedBinding
import com.nunchuk.android.widget.util.inflate

internal class CreateNewSeedAdapter : RecyclerView.Adapter<CreateNewSeedViewHolder>() {

    internal var items: List<String> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CreateNewSeedViewHolder(
        parent.inflate(R.layout.item_new_seed)
    )

    override fun onBindViewHolder(holder: CreateNewSeedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class CreateNewSeedViewHolder(
    itemView: View
) : BaseViewHolder<String>(itemView) {
    private val binding = ItemNewSeedBinding.bind(itemView)

    override fun bind(data: String) {
        binding.seed.text = data
    }
}
