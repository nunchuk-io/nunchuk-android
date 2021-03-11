#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "provider.h"

using namespace nunchuk;

NunchukProvider *NunchukProvider::get() {
    if (!_instance) {
        _instance = new NunchukProvider;
    }
    return _instance;
}

NunchukProvider *NunchukProvider::_instance = nullptr;

void NunchukProvider::initNunchuk(const AppSettings &settings) {
    nu = MakeNunchuk(settings);
}
