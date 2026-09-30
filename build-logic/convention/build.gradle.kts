plugins {
    `kotlin-dsl`
}

group = "com.nunchuk.android.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "nunchuk.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "nunchuk.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("nativeSdk") {
            id = "nunchuk.android.nativesdk"
            implementationClass = "NativeSdkConventionPlugin"
        }
    }
}
