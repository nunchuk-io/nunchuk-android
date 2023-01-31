package com.nunchuk.android.messages.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.nunchuk.android.messages.components.detail.NunchukMedia
import java.io.InputStream

@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(
            NunchukMedia::class.java,
            InputStream::class.java,
            VectorGlideModelLoaderFactory(context)
        )
    }
}