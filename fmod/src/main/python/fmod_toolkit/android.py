from __future__ import annotations

import os
from .errors import FmodToolkitError


def _chaquopy_native_lib_dir() -> str | None:
    """
    Chaquopy-only: ApplicationInfo.nativeLibraryDir
    """
    try:
        from java import jclass  # type: ignore
        Python = jclass("com.chaquo.python.Python")
        app = Python.getPlatform().getApplication()
        return str(app.getApplicationInfo().nativeLibraryDir)
    except Exception:
        return None


def bootstrap(
        native_lib_dir: str | None = None,
        lib_name: str = "libfmod.so",
        force: bool = False
) -> str:
    """
    Sets:
        PYFMODEX_DLL_PATH = <nativeLibraryDir>/libfmod.so

    If native_lib_dir is None, tries Chaquopy auto-detect.
    Raises a clear error if it can't determine a usable path.
    """
    if not force and os.environ.get("PYFMODEX_DLL_PATH"):
        return os.environ["PYFMODEX_DLL_PATH"]

    if native_lib_dir is None:
        native_lib_dir = _chaquopy_native_lib_dir()

    if not native_lib_dir:
        raise FmodToolkitError(
            "Cannot determine Android nativeLibraryDir. "
            "Call fmod_toolkit.bootstrap(native_lib_dir) from Java/Kotlin using "
            "getApplicationInfo().nativeLibraryDir before importing fmod_toolkit."
        )

    path = native_lib_dir.rstrip("/") + "/" + lib_name
    os.environ["PYFMODEX_DLL_PATH"] = path
    return path
