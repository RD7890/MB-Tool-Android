# MB Tool — RenderDragon Shader Tool for Android

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
  <img src="https://img.shields.io/github/v/release/RD7890/MB-Tool-Android?style=for-the-badge&color=3B82F6" />
</p>

**MB Tool** is a fully offline Android app for decompiling and recompiling Minecraft Bedrock Edition's RenderDragon `.material.bin` shader files — no server, no Termux, no root required.

---

## Features

- **Decompile** `.material.bin` → extracts per-pass GLSL/ESSL shader source + metadata JSON
- **Recompile** decompiled folder → rebuilds a valid `.material.bin` (edited GLSL is automatically re-packed)
- **Operation History** — timestamped log of all decompile/recompile operations
- **Theme** — Light / Dark / System (follows your device preference by default)
- **100% Offline** — pure Kotlin port of MaterialBinTool; no network calls
- **No root required** — uses Android SAF (Storage Access Framework) for file access
- **ARM64-only** APK — optimized for modern Android devices

---

## Supported Shader Platforms

| Platform | Decompile | Recompile | Notes |
|----------|-----------|-----------|-------|
| ESSL 100/300/310 | ✅ GLSL extracted | ✅ | Android shaders |
| GLSL 120/430 | ✅ GLSL extracted | ✅ | OpenGL shaders |
| Direct3D SM40–SM65 | ✅ raw blob | ✅ | Windows shaders |
| Metal | ✅ raw blob | ✅ | iOS/macOS shaders |
| Vulkan | ✅ raw blob | ✅ | Switch shaders |

---

## Encryption Support

| Variant | Status |
|---------|--------|
| None | ✅ Full support |
| SimplePassphrase | ✅ Full support |
| KeyPair | ❌ Not supported (RSA-1024 private key required) |

---

## How to Use

### Decompile
1. Open MB Tool → **Home** tab
2. Tap **Input .material.bin** → pick your shader file
3. Tap **Output folder** → pick where to save
4. Tap **Decompile** — a folder is created with `_meta.json` + per-shader `.bin` + `.glsl` files

### Edit Shaders
- Open `.glsl` files in any text editor on your device
- Modify the GLSL/ESSL source code
- Save the file back

### Recompile
1. Back in MB Tool → **Home** tab
2. Tap **Decompiled folder** → pick the decompiled directory
3. Tap **Output folder** → pick where to save the rebuilt `.material.bin`
4. Tap **Recompile** — a new `.material.bin` is written

---

## Engine

MB Tool's core binary parser is a faithful Kotlin port of [MaterialBinTool](https://github.com/ddf8196/MaterialBinTool) by ddf8196 (MIT License). No JNI, no native binaries.

---

## Credits

- **Engine**: [ddf8196/MaterialBinTool](https://github.com/ddf8196/MaterialBinTool) (MIT)
- **Developer**: Rohan Dora (RD7890)
- **UI**: Jetpack Compose · Material 3

---

## License

MIT — see [LICENSE](LICENSE).

> **Disclaimer**: This project is not affiliated with or endorsed by Mojang Studios or Microsoft. RenderDragon is a trademark of Mojang Studios.
