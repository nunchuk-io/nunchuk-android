#include <string.h>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_retrieveData(
        JNIEnv *env,
        jobject thisObj
) {
        syslog(LOG_CRIT, "retrieveData::start");
        AppSettings settings;
        settings.set_chain(Chain::TESTNET);
        settings.set_hwi_path("bin/hwi");
        settings.enable_proxy(false);
        settings.set_testnet_servers({"testnet.nunchuk.io:50001"});
        settings.set_backend_type(BackendType::ELECTRUM);
        // auto nunchuk = MakeNunchuk(settings);
        // auto devices = nunchuk.get()->GetDevices();
        syslog(LOG_CRIT, "retrieveData::end");
}