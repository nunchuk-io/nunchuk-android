import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("nunchuk.android.application")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android"

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        applicationId = "io.nunchuk.android"
        versionCode = 334
        versionName = "2.7.1"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations += "en"
        ndk {
            // abiFilters += listOf("armeabi-v7a", "x86", "arm64-v8a", "x86_64")
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.incremental"] = "true"
            }
        }

        manifestPlaceholders["appAuthRedirectScheme"] = "https"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            } else {
                storeFile = file("${project.rootDir}/dummy.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        getByName("debug") {
            storeFile = file("${project.rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = ".DEV"
            isMinifyEnabled = false
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            multiDexKeepFile = file("multidex-config.txt")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.conf")
            signingConfig = signingConfigs.getByName("release")

            // Disable PNG crunching
            isCrunchPngs = false

            configure<CrashlyticsExtension> {
                // Enable processing and uploading of native symbols to Firebase servers.
                // By default, this is disabled to improve build speeds.
                // This flag must be enabled to see properly-symbolicated native
                // stack traces in the Crashlytics dashboard.
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = file("src/main/obj").path
            }
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("production") {
            applicationId = "io.nunchuk.android"
            dimension = "env"
            resValue("string", "app_name", "Nunchuk")
            configure<AppDistributionExtension> {
                appId = "1:712097058578:android:6ba4711f048801f813edcb"
                releaseNotesFile = "nunchuk-app/release_notes.txt"
                testers = "hung.tran@nunchuk.io"
                groups = "testers"
            }
        }
        create("development") {
            applicationId = "com.nunchuk.android.dev"
            dimension = "env"
            resValue("string", "app_name", "Development")
            configure<AppDistributionExtension> {
                appId = "1:712097058578:android:3a7fe0dc4e6b89b713edcb"
                releaseNotesFile = "nunchuk-app/release_notes.txt"
                testers = "hung.tran@nunchuk.io"
                groups = "testers"
            }
        }
    }
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-auth"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-contact"))
    implementation(project(":nunchuk-database"))
    implementation(project(":nunchuk-main"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-network"))
    implementation(project(":nunchuk-signer"))
    implementation(project(":nunchuk-signer-software"))
    implementation(project(":nunchuk-wallet"))
    implementation(project(":nunchuk-wallet-personal"))
    implementation(project(":nunchuk-wallet-shared"))
    implementation(project(":nunchuk-wallet-core"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-transaction"))
    implementation(project(":nunchuk-messages"))
    implementation(project(":nunchuk-utils"))
    implementation(project(":nunchuk-settings"))
    implementation(project(":nunchuk-notifications"))
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    implementation(libs.matrix.android.sdk2)
    implementation(libs.bundles.workmanager)

    implementation(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.crashlytics.ndk)

    implementation(libs.bundles.lifecycle)

    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.bundles.network)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.bundles.androidx)
    implementation(libs.gson)

    // Kotlin
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.bundles.coroutines)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.landscapist.glide)

    implementation(libs.play.services.base)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.middle.ellipsis.text)
    implementation(libs.bundles.zxing)

    implementation(libs.androidx.multidex)

    implementation(libs.branch)
    implementation(libs.play.services.ads.identifier)
}
