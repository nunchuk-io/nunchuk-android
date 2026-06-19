plugins {
    id("nunchuk.android.library")
}

android {
    namespace = "com.nunchuk.android.utils"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)

    implementation(libs.commons.codec)
}
