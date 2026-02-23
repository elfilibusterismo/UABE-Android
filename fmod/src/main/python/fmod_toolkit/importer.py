from __future__ import annotations

import ctypes
import os

from .android import bootstrap
from .errors import FmodToolkitError

_py = None


def import_pyfmodex(preload_global: bool = True):
    """
    - Ensures PYFMODEX_DLL_PATH is set (tries bootstrap() auto-detect)
    - Optionally preloads libfmod.so with RTLD_GLOBAL
    - Imports pyfmodex
    """
    global _py
    if _py is not None:
        return _py

    if not os.environ.get("PYFMODEX_DLL_PATH"):
        # auto detect via Chaquopy
        bootstrap(None, "libfmod.so", True)

    dll_path = os.environ.get("PYFMODEX_DLL_PATH")
    if not dll_path:
        raise FmodToolkitError("PYFMODEX_DLL_PATH not set (bootstrap failed).")

    # Preload FMOD globally
    if preload_global:
        try:
            ctypes.CDLL(dll_path, mode=ctypes.RTLD_GLOBAL)
        except Exception as e:
            raise FmodToolkitError(
                f"Failed to preload FMOD library via ctypes.CDLL.\n"
                f"Path: {dll_path}\n"
                f"Error: {e}\n"
                f"Make sure libfmod.so exists in jniLibs/<abi>/ and matches your ABI."
            ) from e

    ctypes.windll = None

    try:
        import pyfmodex
    except Exception as e:
        raise FmodToolkitError(
            f"Failed to import pyfmodex on Android.\n"
            f"PYFMODEX_DLL_PATH={dll_path}\n"
            f"Error: {e}\n"
            f"Make sure pyfmodex is packaged for Chaquopy and FMOD lib is the Android one."
        ) from e

    _py = pyfmodex
    return _py
