from __future__ import annotations

import ctypes
import struct
from threading import Lock
from typing import Dict, Optional, Tuple

from .importer import import_pyfmodex

try:
    import numpy as np
except ImportError:
    np = None

pyfmodex = import_pyfmodex(preload_global=True)

SYSTEM_INSTANCES: Dict[Tuple[int, int], Tuple[object, Lock]] = {}
SYSTEM_GLOBAL_LOCK = Lock()


def get_pyfmodex_system_instance(channels: int, init_flags: Optional[int] = None):
    """
    Returns (system, lock) cached per (channels, init_flags).
    """
    if init_flags is None:
        init_flags = int(getattr(pyfmodex.flags.INIT_FLAGS, "NORMAL", 0))

    key = (int(channels), int(init_flags))

    with SYSTEM_GLOBAL_LOCK:
        if key in SYSTEM_INSTANCES:
            return SYSTEM_INSTANCES[key]

        system = pyfmodex.System()
        system.init(int(channels), int(init_flags), None)

        lock = Lock()
        SYSTEM_INSTANCES[key] = (system, lock)
        return system, lock


def raw_to_wav(
        raw_data: bytes,
        name: str,
        channels: int,
        frequency: int,
        convert_pcm_float: bool = True,
) -> Dict[str, bytes]:
    """
    Loads raw bytes via FMOD OPENMEMORY and exports subsounds to WAV.
    """
    system, lock = get_pyfmodex_system_instance(
        channels=channels,
        init_flags=int(getattr(pyfmodex.flags.INIT_FLAGS, "NORMAL", 0)),
    )

    with lock:
        exinfo = pyfmodex.structure_declarations.CREATESOUNDEXINFO(
            length=len(raw_data),
            numchannels=int(channels),
            defaultfrequency=int(frequency),
        )

        sound = system.create_sound(
            bytes(raw_data),
            pyfmodex.flags.MODE.OPENMEMORY,
            exinfo=exinfo,
        )

        out = sound_to_wav(sound, name, convert_pcm_float)
        sound.release()

    return out


def sound_to_wav(sound, name: str, convert_pcm_float: bool = True) -> Dict[str, bytes]:
    """
    Exports subsounds (if present) to WAV.
    If there are no subsounds, exports the sound itself.
    """
    samples: Dict[str, bytes] = {}

    num = int(getattr(sound, "num_subsounds", 0))
    if num <= 0:
        samples[f"{name}.wav"] = subsound_to_wav(sound, convert_pcm_float)
        return samples

    for i in range(num):
        filename = f"{name}.wav" if i == 0 else f"{name}-{i}.wav"
        subs = sound.get_subsound(i)
        samples[filename] = subsound_to_wav(subs, convert_pcm_float)
        subs.release()

    return samples


def subsound_to_wav(subsound, convert_pcm_float: bool = True) -> bytes:
    """
    Exports one FMOD sound to WAV bytes.
    """
    fmt = subsound.format.format
    pcm_bytes = int(subsound.get_length(pyfmodex.enums.TIMEUNIT.PCMBYTES))

    channels = int(subsound.format.channels)
    bits = int(subsound.format.bits)
    sample_rate = int(subsound.default_frequency)

    if fmt in (
            pyfmodex.enums.SOUND_FORMAT.PCM8,
            pyfmodex.enums.SOUND_FORMAT.PCM16,
            pyfmodex.enums.SOUND_FORMAT.PCM24,
            pyfmodex.enums.SOUND_FORMAT.PCM32,
    ):
        audio_format = 1
        wav_data_length = pcm_bytes
        convert_pcm_float = False

    elif fmt == pyfmodex.enums.SOUND_FORMAT.PCMFLOAT:
        if convert_pcm_float:
            audio_format = 1
            bits = 16
            wav_data_length = pcm_bytes // 2
        else:
            audio_format = 3
            wav_data_length = pcm_bytes
    else:
        raise NotImplementedError(f"Sound format {fmt} not supported.")

    raw_wav = bytearray(wav_data_length + 44)

    # RIFF header
    struct.pack_into("<4sI4s", raw_wav, 0, b"RIFF", wav_data_length + 36, b"WAVE")

    # fmt chunk
    struct.pack_into(
        "<4sIHHIIHH",
        raw_wav,
        12,
        b"fmt ",
        16,
        int(audio_format),
        int(channels),
        int(sample_rate),
        int(sample_rate * channels * bits // 8),
        int(channels * bits // 8),
        int(bits),
    )

    # data chunk
    struct.pack_into("<4sI", raw_wav, 36, b"data", int(wav_data_length))

    # payload
    offset = 44
    lock_ranges = subsound.lock(0, pcm_bytes)
    for ptr, length in lock_ranges:
        n = int(getattr(length, "value", length))
        chunk = ctypes.string_at(ptr, n)

        if convert_pcm_float:
            chunk = convert_pcm_float_to_pcm_int16(chunk)

        raw_wav[offset : offset + len(chunk)] = chunk
        offset += len(chunk)

    subsound.unlock(*lock_ranges)

    return bytes(raw_wav)


def convert_pcm_float_to_pcm_int16(ptr_data: bytes) -> bytes:
    if np is not None:
        values = np.frombuffer(ptr_data, dtype=np.float32)
        return (values * (1 << 15)).clip(-32768, 32767).astype(np.int16).tobytes()

    values = struct.unpack("<%df" % (len(ptr_data) // 4), ptr_data)
    v_min = 1 / -32768
    v_max = 1 / 32767
    ints = [
        -32768 if v < v_min else 32767 if v > v_max else int(v * (1 << 15))
        for v in values
    ]
    return struct.pack("<%dh" % len(ints), *ints)


__all__ = [
    "get_pyfmodex_system_instance",
    "raw_to_wav",
    "sound_to_wav",
    "subsound_to_wav",
]
