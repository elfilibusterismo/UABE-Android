# UABE Android

**Unity AssetBundle Editor for Android**  
Built with **UnityPy** via **Chaquopy** to parse and edit Unity AssetBundles directly on Android.

---

## Features
- Open AssetBundles (`.unity3d`, `.bundle`, `.ab`)
- Export:
    - Texture2D → PNG
    - Mesh → OBJ
    - TypeTree → JSON
- Import:
    - PNG → Texture2D
    - TypeTree JSON → Unity object

---

## Requirements
- Android Studio (latest stable recommended)
- JDK 17 (recommended for modern Android Gradle builds)
- Android SDK installed
- NDK + CMake (required to build native decoders)

---

## Setup

### Clone (with submodules)
```bash
git clone --recurse-submodules <YOUR_REPO_URL>
```

### If you already cloned
```bash
git submodule update --init --recursive
```

### Submodules used
```text
third_party/UnityPy   -> https://github.com/K0lb3/UnityPy
third_party/astcenc   -> https://github.com/ARM-software/astc-encoder
third_party/etcpak    -> https://github.com/wolfpld/etcpak
```

---

## FMOD (manual install)
FMOD is **not** included in this repository.

1. Download the **FMOD Engine for Android** package from FMOD.
2. Copy only the required `api/` folder into:
    - `third_party/fmod/`
---

## Model preview (F3D)
This project uses **f3d-android** for model preview.

---

## Build & Run
1. Open the project in **Android Studio**
2. Let Gradle sync finish
3. Run the `app` configuration on a device/emulator

---

## Notes / Limitations
- Very large bundles may fail on low-memory devices
- Encrypted bundles: **not supported yet**

---

## Credits
- UnityPy — https://github.com/K0lb3/UnityPy
- astc-encoder — https://github.com/ARM-software/astc-encoder
- etcpak — https://github.com/wolfpld/etcpak
- f3d-android — https://github.com/f3d-app/f3d-android (model preview)
- Chaquopy — https://github.com/chaquo/chaquopy
- FMOD (Android) — https://www.fmod.com/download#fmodengine

---

## Legal
Unity is a trademark of Unity Technologies. This project is not affiliated with or endorsed by Unity.
