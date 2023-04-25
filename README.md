# Nunchuk Android
Nunchuk Android is a natively written multisig wallet powered by [libnunchuk](https://github.com/nunchuk-io/libnunchuk).

For more info on our products, please visit [our website](https://nunchuk.io/).

# Building the app
## Prerequisite: Building the SDK
Follow the build instructions for [Nunchuk Android Native SDK](https://github.com/nunchuk-io/nunchuk-android-nativesdk). 

Publish the SDK to the local maven. Note the SDK version number.

## Building the app

Open `dependencies.gradle` and update `nativeSdkVersion` to the SDK version you just published.

Build and run the app on your device.

## Alternative solution without building the SDK (Recommended)

You can use prebuilt sdk version from github, check it [here](https://github.com/nunchuk-io/nunchuk-android-nativesdk-prebuild).

Change `nativeSdk` in `dependencies.gradle` to the prebuilt version

[![](https://jitpack.io/v/nunchuk-io/nunchuk-android-nativesdk-prebuild.svg)](https://jitpack.io/#nunchuk-io/nunchuk-android-nativesdk-prebuild)

```
    nunchukDeps = [
            nativeSdk: "com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:latest_version@aar"
    ]
```
