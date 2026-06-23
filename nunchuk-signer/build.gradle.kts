plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.signer"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-network"))
    implementation(project(":nunchuk-database"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-signer-software"))
    implementation("com.github.Nunchuk1:LibPortal:v5@aar")
    implementation("net.java.dev.jna:jna:5.14.0@aar")

    implementation(libs.bundles.network)
    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.zxing)
    implementation(libs.gson)
    implementation(libs.bundles.googleScanner)

    testImplementation(libs.junit)
}
