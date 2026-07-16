package com.rohan.mbtool.engine

/** Ported from MaterialBinTool's ShaderCodePlatform.java (MIT License). */
enum class ShaderCodePlatform {
    Direct3D_SM40,  // Windows
    Direct3D_SM50,  // Windows
    Direct3D_SM60,  // Windows
    Direct3D_SM65,  // Windows
    Direct3D_XB1,
    Direct3D_XBX,
    GLSL_120,
    GLSL_430,
    ESSL_100,       // Android (older)
    ESSL_300,
    ESSL_310,       // Android 1.20+
    Metal,          // iOS/macOS
    Vulkan,         // Switch
    Nvn,
    Pssl,
    Unknown;

    fun isGlsl() = name.startsWith("GLSL") || name.startsWith("ESSL")
    fun isD3D()  = name.startsWith("Direct3D")

    companion object {
        fun get(i: Int) = values().getOrElse(i) { Unknown }
    }
}
