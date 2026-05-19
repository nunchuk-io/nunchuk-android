# Rebuild Native SDK

Pull the latest libnunchuk changes, rebuild the native SDK AAR, publish to local Maven, then build and install the debug APK on the connected device.

## Instructions

Execute the following steps sequentially. Stop and report if any step fails.

### Step 1: Pull latest libnunchuk

```bash
cd /Users/mega_lh/MyProject/nunchuk-android-nativesdk/src/main/native/libnunchuk && git pull
```

Report what changed (files count, insertions/deletions). If already up to date, say so and continue.

### Step 2: Build native SDK AAR

```bash
cd /Users/mega_lh/MyProject/nunchuk-android-nativesdk && ./gradlew assembleArm64_v8aRelease
```

Use a 10-minute timeout. Check the last lines of output to confirm `BUILD SUCCESSFUL`. If the build fails, stop and show the error.

### Step 3: Publish to local Maven

```bash
cd /Users/mega_lh/MyProject/nunchuk-android-nativesdk && ./gradlew publish
```

Confirm `BUILD SUCCESSFUL`.

### Step 4: Build debug APK and install the Nunchuk app

Install the Nunchuk app (`:nunchuk-app`) — NOT the native SDK's test app. Always target the module + flavor explicitly so the task can't be misrouted if cwd shifts.

The app has product flavors (`development`, `production`); `installDebug` alone is ambiguous. Use the `development` flavor for dev work:

```bash
cd /Users/mega_lh/MyProject/my-android && ./gradlew :nunchuk-app:installDevelopmentDebug
```

Use a 10-minute timeout. Confirm the APK was installed on the device.

### Summary

After all steps complete, report a brief summary:
- libnunchuk changes pulled (or "already up to date")
- Native SDK version published
- APK installed successfully (or failure details)
