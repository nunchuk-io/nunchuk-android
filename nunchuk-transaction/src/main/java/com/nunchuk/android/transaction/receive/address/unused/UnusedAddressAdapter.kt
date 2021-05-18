package com.nunchuk.android.transaction.receive.address.unused

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.qr.convertToQRCode
import com.nunchuk.android.transaction.R

class UnusedAddressAdapter(private val context: Context) : PagerAdapter() {

    internal var items = emptyList<String>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = layoutInflater.inflate(R.layout.item_unused_address, null)

        val imageView: ImageView = view.findViewById(R.id.qrCode)
        val textView: TextView = view.findViewById(R.id.address)
        val address = items[position]
        imageView.setImageBitmap(address.convertToQRCode())
        textView.text = address

        val vp = container as ViewPager
        vp.addView(view, 0)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val vp = container as ViewPager
        val view = `object` as View
        vp.removeView(view)
    }

}