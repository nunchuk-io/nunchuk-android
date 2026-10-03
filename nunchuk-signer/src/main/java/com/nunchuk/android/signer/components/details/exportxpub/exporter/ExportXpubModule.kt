package com.nunchuk.android.signer.components.details.exportxpub.exporter

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** One binding per key type that can export an xpub; see [XpubExporter]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface ExportXpubModule {
    @Binds
    fun bindXpubPathRules(rules: SdkXpubPathRules): XpubPathRules

    @Binds
    fun bindSoftwareKeyOperations(operations: SdkSoftwareKeyOperations): SoftwareKeyOperations

    @Binds
    @IntoSet
    fun bindStoredKeyXpubExporter(exporter: StoredKeyXpubExporter): XpubExporter

    @Binds
    @IntoSet
    fun bindSoftwareXpubExporter(exporter: SoftwareXpubExporter): XpubExporter

    @Binds
    @IntoSet
    fun bindTapsignerXpubExporter(exporter: TapsignerXpubExporter): XpubExporter
}
