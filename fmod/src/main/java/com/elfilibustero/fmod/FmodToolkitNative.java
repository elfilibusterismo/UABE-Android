package com.elfilibustero.fmod;

public final class FmodToolkitNative {
    static { System.loadLibrary("uabe_fmod_toolkit"); }
    private FmodToolkitNative() {}

    // -----------------------------
    // Core: System
    // -----------------------------
    public static native long coreSystemCreate();
    public static native void coreSystemInit(long sys, int maxChannels, int initFlags);
    public static native void coreSystemUpdate(long sys);
    public static native void coreSystemRelease(long sys);

    public static native long coreSystemCreateSound(long sys, String path, int mode);
    public static native long coreSystemCreateStream(long sys, String path, int mode);

    public static native long coreSystemPlaySound(long sys, long sound, boolean paused);
    public static native long coreSystemCreateChannelGroup(long sys, String name);
    public static native void coreSystemSetChannelGroup(long sys, long channel, long group);

    // -----------------------------
    // Core: Sound
    // -----------------------------
    public static native void coreSoundRelease(long sound);
    public static native int coreSoundGetLengthMs(long sound);
    public static native void coreSoundSetMode(long sound, int mode);
    public static native int coreSoundGetMode(long sound);

    // -----------------------------
    // Core: ChannelControl (Channel/Group)
    // -----------------------------
    public static native void coreCCSetPaused(long cc, boolean paused);
    public static native boolean coreCCGetPaused(long cc);
    public static native void coreCCSetVolume(long cc, float volume);
    public static native float coreCCGetVolume(long cc);
    public static native void coreCCSetPitch(long cc, float pitch);
    public static native float coreCCGetPitch(long cc);
    public static native void coreCCSetMute(long cc, boolean mute);
    public static native boolean coreCCGetMute(long cc);
    public static native void coreCCStop(long cc);

    // -----------------------------
    // Core: Channel-only
    // -----------------------------
    public static native void coreChannelSetPan(long channel, float pan); // setter only
    public static native boolean coreChannelIsPlaying(long channel);

    // -----------------------------
    // Core: DSP
    // -----------------------------
    public static native long coreSystemCreateDSPByType(long sys, int dspType);
    public static native void coreDSPRelease(long dsp);
    public static native void coreDSPSetBypass(long dsp, boolean bypass);
    public static native boolean coreDSPGetBypass(long dsp);
    public static native void coreDSPSetParameterFloat(long dsp, int index, float value);
    public static native float coreDSPGetParameterFloat(long dsp, int index);

    public static native void coreCCAddDSP(long cc, int index, long dsp);
    public static native void coreCCRemoveDSP(long cc, long dsp);

    // -----------------------------
    // Studio: System
    // -----------------------------
    public static native long studioSystemCreate();
    public static native void studioSystemInitialize(long studio, int maxChannels, int studioFlags, int coreInitFlags);
    public static native void studioSystemUpdate(long studio);
    public static native void studioSystemRelease(long studio);

    public static native long studioSystemGetCoreSystem(long studio);

    // -----------------------------
    // Studio: Bank
    // -----------------------------
    public static native long studioSystemLoadBankFile(long studio, String path, int loadFlags);
    public static native void studioBankUnload(long bank);

    // -----------------------------
    // Studio: Event
    // -----------------------------
    public static native long studioSystemGetEvent(long studio, String eventPath);
    public static native long studioEventDescCreateInstance(long eventDesc);

    public static native void studioEventInstanceStart(long eventInst);
    public static native void studioEventInstanceStop(long eventInst, int stopMode);
    public static native void studioEventInstanceRelease(long eventInst);

    public static native void studioEventInstanceSetParameterByName(long eventInst, String name, float value, boolean ignoreSeekSpeed);

    // -----------------------------
    // Studio: Bus / VCA
    // -----------------------------
    public static native long studioSystemGetBus(long studio, String busPath);
    public static native void studioBusStopAllEvents(long bus, int stopMode);
    public static native void studioBusSetMute(long bus, boolean mute);
    public static native void studioBusSetVolume(long bus, float volume);

    public static native long studioSystemGetVCA(long studio, String vcaPath);
    public static native void studioVCASetVolume(long vca, float volume);
}
