#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

class NunchukProvider {
    static NunchukProvider *_instance;

private:
    NunchukProvider() {
        syslog(LOG_CRIT, "[JNI]created NunchukProvider");
    }

public:
    static NunchukProvider *get();

    std::unique_ptr<Nunchuk> nu;

    void initNunchuk(const AppSettings &settings);
};