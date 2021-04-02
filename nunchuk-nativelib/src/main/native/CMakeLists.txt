cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(ANDROID_TARGET "aarch64-linux-android")
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(ANDROID_TARGET "armv7a-linux-androideabi")
else ()
    set(ANDROID_TARGET "${ANDROID_ABI}-linux-android")
endif ()

message("ANDROID_ABI::${ANDROID_ABI},ANDROID_TARGET::${ANDROID_TARGET}")

set(UR__DISABLE_TESTS ON)

add_library(nunchuk-android SHARED
        deserializer.cpp
        serializer.cpp
        provider.cpp
        initializer-jni.cpp
        wallet-jni.cpp
        signer-jni.cpp
)

add_subdirectory(libnunchuk)

target_link_libraries(nunchuk-android
        nunchuk)

target_include_directories(nunchuk-android PUBLIC
        "${PROJECT_SOURCE_DIR}"
        "${PROJECT_SOURCE_DIR}/libnunchuk/include"
        "${PROJECT_SOURCE_DIR}/contrib/bitcoin/src"
        "${PROJECT_SOURCE_DIR}/contrib/bitcoin/src/univalue/include"
        "${PROJECT_SOURCE_DIR}/contrib/bitcoin/src/leveldb/include"
        "${PROJECT_SOURCE_DIR}/contrib/bitcoin/depends/${ANDROID_TARGET}/include")
