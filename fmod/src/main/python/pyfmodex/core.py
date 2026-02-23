from __future__ import annotations
from ._native import Native

FMOD_INIT_NORMAL = 0
FMOD_DEFAULT = 0

class System:
    def __init__(self):
        self.ptr = int(Native.coreSystemCreate())

    def init(self, maxchannels: int = 512, init_flags: int = FMOD_INIT_NORMAL):
        Native.coreSystemInit(self.ptr, int(maxchannels), int(init_flags))
        return self

    def update(self):
        Native.coreSystemUpdate(self.ptr)

    def release(self):
        if self.ptr:
            Native.coreSystemRelease(self.ptr)
            self.ptr = 0

    def create_sound(self, path: str, mode: int = FMOD_DEFAULT) -> "Sound":
        sp = int(Native.coreSystemCreateSound(self.ptr, path, int(mode)))
        return Sound(self, sp)

    def create_stream(self, path: str, mode: int = FMOD_DEFAULT) -> "Sound":
        sp = int(Native.coreSystemCreateStream(self.ptr, path, int(mode)))
        return Sound(self, sp)

    def play_sound(self, sound: "Sound", paused: bool = False) -> "Channel":
        cp = int(Native.coreSystemPlaySound(self.ptr, int(sound.ptr), bool(paused)))
        return Channel(cp)

    def create_channel_group(self, name: str) -> "ChannelGroup":
        gp = int(Native.coreSystemCreateChannelGroup(self.ptr, name))
        return ChannelGroup(gp)

    def set_channel_group(self, channel: "Channel", group: "ChannelGroup"):
        Native.coreSystemSetChannelGroup(self.ptr, int(channel.ptr), int(group.ptr))

    def create_dsp_by_type(self, dsp_type: int) -> "DSP":
        dp = int(Native.coreSystemCreateDSPByType(self.ptr, int(dsp_type)))
        return DSP(dp)

    def __enter__(self): return self
    def __exit__(self, exc_type, exc, tb):
        try: self.release()
        except Exception: pass
        return False


class Sound:
    def __init__(self, system: System, ptr: int):
        self.sys = system
        self.ptr = int(ptr)

    def play(self, paused: bool = False) -> "Channel":
        cp = int(Native.coreSystemPlaySound(self.sys.ptr, self.ptr, bool(paused)))
        return Channel(cp)

    def release(self):
        if self.ptr:
            Native.coreSoundRelease(self.ptr)
            self.ptr = 0

    def get_length_ms(self) -> int:
        return int(Native.coreSoundGetLengthMs(self.ptr))

    def get_mode(self) -> int:
        return int(Native.coreSoundGetMode(self.ptr))

    def set_mode(self, mode: int):
        Native.coreSoundSetMode(self.ptr, int(mode))


class _ChannelControl:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def stop(self):
        Native.coreCCStop(self.ptr)

    def set_paused(self, v: bool):
        Native.coreCCSetPaused(self.ptr, bool(v))

    def get_paused(self) -> bool:
        return bool(Native.coreCCGetPaused(self.ptr))

    def set_volume(self, v: float):
        Native.coreCCSetVolume(self.ptr, float(v))

    def get_volume(self) -> float:
        return float(Native.coreCCGetVolume(self.ptr))

    def set_pitch(self, v: float):
        Native.coreCCSetPitch(self.ptr, float(v))

    def get_pitch(self) -> float:
        return float(Native.coreCCGetPitch(self.ptr))

    def set_mute(self, v: bool):
        Native.coreCCSetMute(self.ptr, bool(v))

    def get_mute(self) -> bool:
        return bool(Native.coreCCGetMute(self.ptr))

    def add_dsp(self, dsp: "DSP", index: int = 0):
        Native.coreCCAddDSP(self.ptr, int(index), int(dsp.ptr))

    def remove_dsp(self, dsp: "DSP"):
        Native.coreCCRemoveDSP(self.ptr, int(dsp.ptr))


class Channel(_ChannelControl):
    @property
    def is_playing(self) -> bool:
        return bool(Native.coreChannelIsPlaying(self.ptr))

    def set_pan(self, v: float):
        Native.coreChannelSetPan(self.ptr, float(v))


class ChannelGroup(_ChannelControl):
    pass


class DSP:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def release(self):
        if self.ptr:
            Native.coreDSPRelease(self.ptr)
            self.ptr = 0

    def set_bypass(self, v: bool):
        Native.coreDSPSetBypass(self.ptr, bool(v))

    def get_bypass(self) -> bool:
        return bool(Native.coreDSPGetBypass(self.ptr))

    def set_parameter_float(self, idx: int, v: float):
        Native.coreDSPSetParameterFloat(self.ptr, int(idx), float(v))

    def get_parameter_float(self, idx: int) -> float:
        return float(Native.coreDSPGetParameterFloat(self.ptr, int(idx)))
