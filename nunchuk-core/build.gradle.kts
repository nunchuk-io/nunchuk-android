plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.core"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-network"))
    implementation(project(":nunchuk-utils"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-database"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.matrix.android.sdk2)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)

    implementation(libs.bundles.zxing)
    implementation(libs.bundles.googleScanner)

    implementation(libs.moshi)
    implementation("com.github.Nunchuk1:LibPortal:v5@aar")
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.androidx.credentials)
    testImplementation(libs.junit)
}
