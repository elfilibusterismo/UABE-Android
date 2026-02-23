from __future__ import annotations

__version__ = "0.1.3"

from .android import bootstrap
bootstrap(None, "libfmod.so", False)

from .fmod import (
    get_pyfmodex_system_instance,
    raw_to_wav,
    sound_to_wav,
    subsound_to_wav,
)

__all__ = [
    "bootstrap",
    "get_pyfmodex_system_instance",
    "raw_to_wav",
    "sound_to_wav",
    "subsound_to_wav",
    "__version__",
]
