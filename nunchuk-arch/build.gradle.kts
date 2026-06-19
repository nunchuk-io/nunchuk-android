plugins {
    id("nunchuk.android.library")
}

android {
    namespace = "com.nunchuk.android.arch"
}

dependencies {
    implementation(project(":nunchuk-utils"))
    implementation(libs.bundles.lifecycle)
}
