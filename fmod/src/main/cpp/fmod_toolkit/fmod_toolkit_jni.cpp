#include <jni.h>
#include <android/log.h>
#include <string>

#include "fmod.hpp"
#include "fmod_errors.h"

#include "fmod_studio.hpp"
#include "fmod_studio_common.h"

#define LOG_TAG "UABE_FMOD"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static jclass gExClass = nullptr;

static void throwFmod(JNIEnv* env, const char* where, FMOD_RESULT r) {
    if (r == FMOD_OK) return;

    char msg[512];
    snprintf(msg, sizeof(msg), "%s failed: (%d) %s", where, (int)r, FMOD_ErrorString(r));
    ALOGE("%s", msg);

    if (!gExClass) {
        jclass local = env->FindClass("com/elfilibustero/fmod/FmodException");
        gExClass = (jclass)env->NewGlobalRef(local);
        env->DeleteLocalRef(local);
    }
    env->ThrowNew(gExClass, msg);
}

static std::string jstr(JNIEnv* env, jstring s) {
    if (!s) return {};
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out = c ? c : "";
    env->ReleaseStringUTFChars(s, c);
    return out;
}

static inline FMOD::System* asCoreSys(jlong p) { return (FMOD::System*)p; }
static inline FMOD::Sound* asSound(jlong p) { return (FMOD::Sound*)p; }
static inline FMOD::Channel* asChannel(jlong p) { return (FMOD::Channel*)p; }
static inline FMOD::ChannelGroup* asGroup(jlong p) { return (FMOD::ChannelGroup*)p; }
static inline FMOD::ChannelControl* asCC(jlong p) { return (FMOD::ChannelControl*)p; }
static inline FMOD::DSP* asDSP(jlong p) { return (FMOD::DSP*)p; }

static inline FMOD::Studio::System* asStudioSys(jlong p) { return (FMOD::Studio::System*)p; }
static inline FMOD::Studio::Bank* asBank(jlong p) { return (FMOD::Studio::Bank*)p; }
static inline FMOD::Studio::EventDescription* asED(jlong p) { return (FMOD::Studio::EventDescription*)p; }
static inline FMOD::Studio::EventInstance* asEI(jlong p) { return (FMOD::Studio::EventInstance*)p; }
static inline FMOD::Studio::Bus* asBus(jlong p) { return (FMOD::Studio::Bus*)p; }
static inline FMOD::Studio::VCA* asVCA(jlong p) { return (FMOD::Studio::VCA*)p; }

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemCreate(JNIEnv* env, jclass) {
    FMOD::System* sys = nullptr;
    FMOD_RESULT r = FMOD::System_Create(&sys);
    throwFmod(env, "FMOD::System_Create", r);
    return (jlong)sys;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemInit(JNIEnv* env, jclass,
                                                             jlong sysPtr, jint maxChannels, jint initFlags) {
    auto* sys = asCoreSys(sysPtr);
    FMOD_RESULT r = sys->init((int)maxChannels, (FMOD_INITFLAGS)initFlags, nullptr);
    throwFmod(env, "System::init", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemUpdate(JNIEnv* env, jclass, jlong sysPtr) {
    auto* sys = asCoreSys(sysPtr);
    FMOD_RESULT r = sys->update();
    throwFmod(env, "System::update", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemRelease(JNIEnv* env, jclass, jlong sysPtr) {
    auto* sys = asCoreSys(sysPtr);
    if (!sys) return;
    FMOD_RESULT r = sys->release();
    throwFmod(env, "System::release", r);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemCreateSound(JNIEnv* env, jclass,
                                                                    jlong sysPtr, jstring path, jint mode) {
    auto* sys = asCoreSys(sysPtr);
    std::string p = jstr(env, path);

    FMOD::Sound* snd = nullptr;
    FMOD_RESULT r = sys->createSound(p.c_str(), (FMOD_MODE)mode, nullptr, &snd);
    throwFmod(env, "System::createSound", r);
    return (jlong)snd;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemCreateStream(JNIEnv* env, jclass,
                                                                     jlong sysPtr, jstring path, jint mode) {
    auto* sys = asCoreSys(sysPtr);
    std::string p = jstr(env, path);

    FMOD::Sound* snd = nullptr;
    FMOD_RESULT r = sys->createStream(p.c_str(), (FMOD_MODE)mode, nullptr, &snd);
    throwFmod(env, "System::createStream", r);
    return (jlong)snd;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemPlaySound(JNIEnv* env, jclass,
                                                                  jlong sysPtr, jlong soundPtr, jboolean paused) {
    auto* sys = asCoreSys(sysPtr);
    auto* snd = asSound(soundPtr);

    FMOD::Channel* ch = nullptr;
    FMOD_RESULT r = sys->playSound(snd, nullptr, paused ? true : false, &ch);
    throwFmod(env, "System::playSound", r);
    return (jlong)ch;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemCreateChannelGroup(JNIEnv* env, jclass,
                                                                           jlong sysPtr, jstring name) {
    auto* sys = asCoreSys(sysPtr);
    std::string n = jstr(env, name);

    FMOD::ChannelGroup* g = nullptr;
    FMOD_RESULT r = sys->createChannelGroup(n.c_str(), &g);
    throwFmod(env, "System::createChannelGroup", r);
    return (jlong)g;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemSetChannelGroup(JNIEnv* env, jclass,
                                                                        jlong /*sysPtr*/, jlong chPtr, jlong grpPtr) {
    auto* ch = asChannel(chPtr);
    auto* g = asGroup(grpPtr);
    FMOD_RESULT r = ch->setChannelGroup(g);
    throwFmod(env, "Channel::setChannelGroup", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSoundRelease(JNIEnv* env, jclass, jlong soundPtr) {
    auto* s = asSound(soundPtr);
    if (!s) return;
    FMOD_RESULT r = s->release();
    throwFmod(env, "Sound::release", r);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSoundGetLengthMs(JNIEnv* env, jclass, jlong soundPtr) {
    auto* s = asSound(soundPtr);
    unsigned int len = 0;
    FMOD_RESULT r = s->getLength(&len, FMOD_TIMEUNIT_MS);
    throwFmod(env, "Sound::getLength(MS)", r);
    return (jint)len;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSoundSetMode(JNIEnv* env, jclass, jlong soundPtr, jint mode) {
    auto* s = asSound(soundPtr);
    FMOD_RESULT r = s->setMode((FMOD_MODE)mode);
    throwFmod(env, "Sound::setMode", r);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSoundGetMode(JNIEnv* env, jclass, jlong soundPtr) {
    auto* s = asSound(soundPtr);
    FMOD_MODE mode = 0;
    FMOD_RESULT r = s->getMode(&mode);
    throwFmod(env, "Sound::getMode", r);
    return (jint)mode;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCSetPaused(JNIEnv* env, jclass, jlong ccPtr, jboolean paused) {
    auto* cc = asCC(ccPtr);
    FMOD_RESULT r = cc->setPaused(paused ? true : false);
    throwFmod(env, "ChannelControl::setPaused", r);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCGetPaused(JNIEnv* env, jclass, jlong ccPtr) {
    auto* cc = asCC(ccPtr);
    bool p = false;
    FMOD_RESULT r = cc->getPaused(&p);
    throwFmod(env, "ChannelControl::getPaused", r);
    return p ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCSetVolume(JNIEnv* env, jclass, jlong ccPtr, jfloat v) {
    auto* cc = asCC(ccPtr);
    FMOD_RESULT r = cc->setVolume((float)v);
    throwFmod(env, "ChannelControl::setVolume", r);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCGetVolume(JNIEnv* env, jclass, jlong ccPtr) {
    auto* cc = asCC(ccPtr);
    float v = 0.f;
    FMOD_RESULT r = cc->getVolume(&v);
    throwFmod(env, "ChannelControl::getVolume", r);
    return (jfloat)v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCSetPitch(JNIEnv* env, jclass, jlong ccPtr, jfloat v) {
    auto* cc = asCC(ccPtr);
    FMOD_RESULT r = cc->setPitch((float)v);
    throwFmod(env, "ChannelControl::setPitch", r);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCGetPitch(JNIEnv* env, jclass, jlong ccPtr) {
    auto* cc = asCC(ccPtr);
    float v = 0.f;
    FMOD_RESULT r = cc->getPitch(&v);
    throwFmod(env, "ChannelControl::getPitch", r);
    return (jfloat)v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCSetMute(JNIEnv* env, jclass, jlong ccPtr, jboolean mute) {
    auto* cc = asCC(ccPtr);
    FMOD_RESULT r = cc->setMute(mute ? true : false);
    throwFmod(env, "ChannelControl::setMute", r);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCGetMute(JNIEnv* env, jclass, jlong ccPtr) {
    auto* cc = asCC(ccPtr);
    bool v = false;
    FMOD_RESULT r = cc->getMute(&v);
    throwFmod(env, "ChannelControl::getMute", r);
    return v ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCStop(JNIEnv* env, jclass, jlong ccPtr) {
    auto* cc = asCC(ccPtr);
    FMOD_RESULT r = cc->stop();
    throwFmod(env, "ChannelControl::stop", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreChannelSetPan(JNIEnv* env, jclass, jlong chPtr, jfloat v) {
    auto* ch = asChannel(chPtr);
    FMOD_RESULT r = ch->setPan((float)v);
    throwFmod(env, "Channel::setPan", r);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreChannelIsPlaying(JNIEnv* env, jclass, jlong chPtr) {
    auto* ch = asChannel(chPtr);
    bool playing = false;
    FMOD_RESULT r = ch->isPlaying(&playing);
    throwFmod(env, "Channel::isPlaying", r);
    return playing ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreSystemCreateDSPByType(JNIEnv* env, jclass,
                                                                        jlong sysPtr, jint dspType) {
    auto* sys = asCoreSys(sysPtr);
    FMOD::DSP* dsp = nullptr;
    FMOD_RESULT r = sys->createDSPByType((FMOD_DSP_TYPE)dspType, &dsp);
    throwFmod(env, "System::createDSPByType", r);
    return (jlong)dsp;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreDSPRelease(JNIEnv* env, jclass, jlong dspPtr) {
    auto* dsp = asDSP(dspPtr);
    if (!dsp) return;
    FMOD_RESULT r = dsp->release();
    throwFmod(env, "DSP::release", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreDSPSetBypass(JNIEnv* env, jclass, jlong dspPtr, jboolean bypass) {
    auto* dsp = asDSP(dspPtr);
    FMOD_RESULT r = dsp->setBypass(bypass ? true : false);
    throwFmod(env, "DSP::setBypass", r);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreDSPGetBypass(JNIEnv* env, jclass, jlong dspPtr) {
    auto* dsp = asDSP(dspPtr);
    bool b = false;
    FMOD_RESULT r = dsp->getBypass(&b);
    throwFmod(env, "DSP::getBypass", r);
    return b ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreDSPSetParameterFloat(JNIEnv* env, jclass,
                                                                       jlong dspPtr, jint idx, jfloat val) {
    auto* dsp = asDSP(dspPtr);
    FMOD_RESULT r = dsp->setParameterFloat((int)idx, (float)val);
    throwFmod(env, "DSP::setParameterFloat", r);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreDSPGetParameterFloat(JNIEnv* env, jclass,
                                                                       jlong dspPtr, jint idx) {
    auto* dsp = asDSP(dspPtr);
    float v = 0.f;
    FMOD_RESULT r = dsp->getParameterFloat((int)idx, &v, nullptr, 0);
    throwFmod(env, "DSP::getParameterFloat", r);
    return (jfloat)v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCAddDSP(JNIEnv* env, jclass,
                                                           jlong ccPtr, jint index, jlong dspPtr) {
    auto* cc = asCC(ccPtr);
    auto* dsp = asDSP(dspPtr);

    // Older FMOD C++ wrapper expects only (index, dsp)
    FMOD_RESULT r = cc->addDSP((int)index, dsp);
    throwFmod(env, "ChannelControl::addDSP", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_coreCCRemoveDSP(JNIEnv* env, jclass,
                                                              jlong ccPtr, jlong dspPtr) {
    auto* cc = asCC(ccPtr);
    auto* dsp = asDSP(dspPtr);

    int count = 0;
    FMOD_RESULT r = cc->getNumDSPs(&count);
    throwFmod(env, "ChannelControl::getNumDSPs", r);

    for (int i = 0; i < count; i++) {
        FMOD::DSP* cur = nullptr;
        r = cc->getDSP(i, &cur);
        throwFmod(env, "ChannelControl::getDSP", r);
        if (cur == dsp) {
            r = cc->removeDSP(cur);
            throwFmod(env, "ChannelControl::removeDSP", r);
            return;
        }
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemCreate(JNIEnv* env, jclass) {
    FMOD::Studio::System* s = nullptr;
    FMOD_RESULT r = FMOD::Studio::System::create(&s);
    throwFmod(env, "Studio::System::create", r);
    return (jlong)s;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemInitialize(JNIEnv* env, jclass,
                                                                     jlong studioPtr,
                                                                     jint maxChannels,
                                                                     jint studioFlags,
                                                                     jint coreInitFlags) {
    auto* s = asStudioSys(studioPtr);
    FMOD_RESULT r = s->initialize((int)maxChannels,
                                  (FMOD_STUDIO_INITFLAGS)studioFlags,
                                  (FMOD_INITFLAGS)coreInitFlags,
                                  nullptr);
    throwFmod(env, "StudioSystem::initialize", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemUpdate(JNIEnv* env, jclass, jlong studioPtr) {
    auto* s = asStudioSys(studioPtr);
    FMOD_RESULT r = s->update();
    throwFmod(env, "StudioSystem::update", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemRelease(JNIEnv* env, jclass, jlong studioPtr) {
    auto* s = asStudioSys(studioPtr);
    if (!s) return;
    FMOD_RESULT r = s->release();
    throwFmod(env, "StudioSystem::release", r);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemGetCoreSystem(JNIEnv* env, jclass, jlong studioPtr) {
    auto* s = asStudioSys(studioPtr);
    FMOD::System* core = nullptr;
    FMOD_RESULT r = s->getCoreSystem(&core);
    throwFmod(env, "StudioSystem::getCoreSystem", r);
    return (jlong)core;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemLoadBankFile(JNIEnv* env, jclass,
                                                                       jlong studioPtr, jstring path, jint flags) {
    auto* s = asStudioSys(studioPtr);
    std::string p = jstr(env, path);

    FMOD::Studio::Bank* bank = nullptr;
    FMOD_RESULT r = s->loadBankFile(p.c_str(), (FMOD_STUDIO_LOAD_BANK_FLAGS)flags, &bank);
    throwFmod(env, "StudioSystem::loadBankFile", r);
    return (jlong)bank;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioBankUnload(JNIEnv* env, jclass, jlong bankPtr) {
    auto* b = asBank(bankPtr);
    if (!b) return;
    FMOD_RESULT r = b->unload();
    throwFmod(env, "Bank::unload", r);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemGetEvent(JNIEnv* env, jclass,
                                                                   jlong studioPtr, jstring eventPath) {
    auto* s = asStudioSys(studioPtr);
    std::string p = jstr(env, eventPath);

    FMOD::Studio::EventDescription* ed = nullptr;
    FMOD_RESULT r = s->getEvent(p.c_str(), &ed);
    throwFmod(env, "StudioSystem::getEvent", r);
    return (jlong)ed;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioEventDescCreateInstance(JNIEnv* env, jclass, jlong edPtr) {
    auto* ed = asED(edPtr);
    FMOD::Studio::EventInstance* inst = nullptr;
    FMOD_RESULT r = ed->createInstance(&inst);
    throwFmod(env, "EventDescription::createInstance", r);
    return (jlong)inst;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioEventInstanceStart(JNIEnv* env, jclass, jlong instPtr) {
    auto* inst = asEI(instPtr);
    FMOD_RESULT r = inst->start();
    throwFmod(env, "EventInstance::start", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioEventInstanceStop(JNIEnv* env, jclass, jlong instPtr, jint stopMode) {
    auto* inst = asEI(instPtr);
    FMOD_RESULT r = inst->stop((FMOD_STUDIO_STOP_MODE)stopMode);
    throwFmod(env, "EventInstance::stop", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioEventInstanceRelease(JNIEnv* env, jclass, jlong instPtr) {
    auto* inst = asEI(instPtr);
    if (!inst) return;
    FMOD_RESULT r = inst->release();
    throwFmod(env, "EventInstance::release", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioEventInstanceSetParameterByName(JNIEnv* env, jclass,
                                                                                    jlong instPtr,
                                                                                    jstring name,
                                                                                    jfloat value,
                                                                                    jboolean ignoreSeek) {
    auto* inst = asEI(instPtr);
    std::string n = jstr(env, name);
    FMOD_RESULT r = inst->setParameterByName(n.c_str(), (float)value, ignoreSeek ? true : false);
    throwFmod(env, "EventInstance::setParameterByName", r);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemGetBus(JNIEnv* env, jclass,
                                                                 jlong studioPtr, jstring busPath) {
    auto* s = asStudioSys(studioPtr);
    std::string p = jstr(env, busPath);

    FMOD::Studio::Bus* bus = nullptr;
    FMOD_RESULT r = s->getBus(p.c_str(), &bus);
    throwFmod(env, "StudioSystem::getBus", r);
    return (jlong)bus;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioBusStopAllEvents(JNIEnv* env, jclass, jlong busPtr, jint stopMode) {
    auto* b = asBus(busPtr);
    FMOD_RESULT r = b->stopAllEvents((FMOD_STUDIO_STOP_MODE)stopMode);
    throwFmod(env, "Bus::stopAllEvents", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioBusSetMute(JNIEnv* env, jclass, jlong busPtr, jboolean mute) {
    auto* b = asBus(busPtr);
    FMOD_RESULT r = b->setMute(mute ? true : false);
    throwFmod(env, "Bus::setMute", r);
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioBusSetVolume(JNIEnv* env, jclass, jlong busPtr, jfloat vol) {
    auto* b = asBus(busPtr);
    FMOD_RESULT r = b->setVolume((float)vol);
    throwFmod(env, "Bus::setVolume", r);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioSystemGetVCA(JNIEnv* env, jclass,
                                                                 jlong studioPtr, jstring vcaPath) {
    auto* s = asStudioSys(studioPtr);
    std::string p = jstr(env, vcaPath);

    FMOD::Studio::VCA* v = nullptr;
    FMOD_RESULT r = s->getVCA(p.c_str(), &v);
    throwFmod(env, "StudioSystem::getVCA", r);
    return (jlong)v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_elfilibustero_fmod_FmodToolkitNative_studioVCASetVolume(JNIEnv* env, jclass, jlong vcaPtr, jfloat vol) {
    auto* v = asVCA(vcaPtr);
    FMOD_RESULT r = v->setVolume((float)vol);
    throwFmod(env, "VCA::setVolume", r);
}
