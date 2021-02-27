# Nunchuk Android

## Getting Started

## Installing

1. Android Build Systems (https://developer.android.com/ndk/guides/other_build_systems)
    - ```armeabi-v7a```: ```armv7a-linux-androideabi```
    - ```arm64-v8a```: ```aarch64-linux-android```
    - ```x86```: ```i686-linux-android```
    - ```x86-64```: ```x86_64-linux-android```


2. Add libnunchuk to nunchuk-nativelib module
    - path to ```nunchuk-nativelib/src/main/native```
    - ```git submodule add -b android git@gitlab.com:nunchuck/libnunchuk.git```
    - ```git submodule update --init --recursive```


3. Build bitcoin deps:
    - path to ```nunchuk-nativelib/src/main/native/libnunchuk/contrib/bitcoin/depends```
    -  ```ANDROID_SDK=/usr/local/android-sdk ANDROID_NDK=/usr/local/android-sdk/ndk/21.0.6113669 make HOST=arm-linux-androideabi ANDROID_API_LEVEL=29 ANDROID_TOOLCHAIN_BIN=/usr/local/android-sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/darwin-x86_64/bin NO_QT=1 NO_ZMQ=1 NO_QR=1 NO_UPNP=1```


4. Build bitcoin deps:
    - ```./autogen.sh```
    - ```CFLAGS="-fPIC" CXXFLAGS="-fPIC" ./configure --prefix=$PWD/depends/arm-linux-androideabi --without-gui --disable-zmq --with-miniupnpc=no --with-incompatible-bdb --disable-bench --disable-tests```
    - ```make```
   

5. Build openssl: (https://github.com/openssl/openssl/blob/master/NOTES-ANDROID.md)
    - ```export ANDROID_NDK_ROOT=/usr/local/android-sdk/ndk/21.0.6113669```
    - ```PATH=$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/i686-linux-android/bin:$ANDROID_NDK_ROOT/toolchains/x86-4.9/prebuilt/darwin-x86_64/bin:$PATH```
    - ```./Configure android-x86 -D__ANDROID_API__=29```
    - ```make```
   

6. Build systems and dependencies:
    - MacOS: ```10.15```
    - CMake: ```3.10.2```
    - NDK: ```21.0.6113669```
    - Boost: ```1.70.0```
    - API: ```29```


## Auto build deps
   - ```nunchuk-nativelib/src/main/native```
   - ```.install_deps.sh ${abi}```

