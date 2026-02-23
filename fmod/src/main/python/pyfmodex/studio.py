from __future__ import annotations
from ._native import Native

STUDIO_INIT_NORMAL = 0
LOAD_BANK_NORMAL = 0

STOP_ALLOWFADEOUT = 0
STOP_IMMEDIATE = 1

class StudioSystem:
    def __init__(self):
        self.ptr = int(Native.studioSystemCreate())

    def initialize(self, maxchannels: int = 1024, studio_flags: int = STUDIO_INIT_NORMAL, core_init_flags: int = 0):
        Native.studioSystemInitialize(self.ptr, int(maxchannels), int(studio_flags), int(core_init_flags))
        return self

    def update(self):
        Native.studioSystemUpdate(self.ptr)

    def release(self):
        if self.ptr:
            Native.studioSystemRelease(self.ptr)
            self.ptr = 0

    def get_core_system_ptr(self) -> int:
        return int(Native.studioSystemGetCoreSystem(self.ptr))

    def load_bank_file(self, path: str, flags: int = LOAD_BANK_NORMAL) -> "Bank":
        bp = int(Native.studioSystemLoadBankFile(self.ptr, path, int(flags)))
        return Bank(bp)

    def get_event(self, event_path: str) -> "EventDescription":
        ep = int(Native.studioSystemGetEvent(self.ptr, event_path))
        return EventDescription(ep)

    def get_bus(self, bus_path: str) -> "Bus":
        bp = int(Native.studioSystemGetBus(self.ptr, bus_path))
        return Bus(bp)

    def get_vca(self, vca_path: str) -> "VCA":
        vp = int(Native.studioSystemGetVCA(self.ptr, vca_path))
        return VCA(vp)

    def __enter__(self): return self
    def __exit__(self, exc_type, exc, tb):
        try: self.release()
        except Exception: pass
        return False


class Bank:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def unload(self):
        if self.ptr:
            Native.studioBankUnload(self.ptr)
            self.ptr = 0


class EventDescription:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def create_instance(self) -> "EventInstance":
        ip = int(Native.studioEventDescCreateInstance(self.ptr))
        return EventInstance(ip)


class EventInstance:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def start(self):
        Native.studioEventInstanceStart(self.ptr)

    def stop(self, immediate: bool = False):
        Native.studioEventInstanceStop(self.ptr, STOP_IMMEDIATE if immediate else STOP_ALLOWFADEOUT)

    def set_parameter_by_name(self, name: str, value: float, ignore_seek_speed: bool = False):
        Native.studioEventInstanceSetParameterByName(self.ptr, name, float(value), bool(ignore_seek_speed))

    def release(self):
        if self.ptr:
            Native.studioEventInstanceRelease(self.ptr)
            self.ptr = 0


class Bus:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def stop_all_events(self, immediate: bool = False):
        Native.studioBusStopAllEvents(self.ptr, STOP_IMMEDIATE if immediate else STOP_ALLOWFADEOUT)

    def set_mute(self, mute: bool):
        Native.studioBusSetMute(self.ptr, bool(mute))

    def set_volume(self, vol: float):
        Native.studioBusSetVolume(self.ptr, float(vol))


class VCA:
    def __init__(self, ptr: int):
        self.ptr = int(ptr)

    def set_volume(self, vol: float):
        Native.studioVCASetVolume(self.ptr, float(vol))
