#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "provider.h"
#include "serializer.h"
#include "deserializer.h"

using namespace nunchuk;

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_initNunchuk(
        JNIEnv *env,
        jobject thiz,
        jint chain,
        jstring hwi_path,
        jboolean enable_proxy,
        jobject testnet_servers,
        jint backend_type,
        jstring storage_path
) {
    syslog(LOG_DEBUG, "[JNI][initNunchuk]initNunchuk()");
    AppSettings settings;
    settings.set_chain(Serializer::convert2CChain(chain));
    settings.set_hwi_path(env->GetStringUTFChars(hwi_path, nullptr));
    settings.enable_proxy(enable_proxy);
    settings.set_testnet_servers({"testnet.nunchuk.io:50001"});
    settings.set_backend_type(Serializer::convert2CBackendType(backend_type));
    settings.set_storage_path(env->GetStringUTFChars(storage_path, nullptr));
    syslog(LOG_DEBUG, "[JNI][initNunchuk]storage path:: %s", settings.get_storage_path().c_str());
    NunchukProvider::get()->initNunchuk(settings);
}

