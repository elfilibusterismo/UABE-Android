class ASTCProfile:
    # Keep the same names
    LDR_SRGB = 0
    LDR = 1
    HDR_RGB_LDR_A = 2
    HDR = 3


class ASTCType:
    # UnityPy uses U8
    U8 = 0
    F16 = 1
    F32 = 2


class ASTCConfigFlags:
    """
    Upstream exposes a flags enum. UnityPy uses USE_DECODE_UNORM8.
    Values don't matter for compatibility, but keep stable integers.
    """
    NONE = 0

    # UnityPy passes this when decoding to ensure unorm8 output
    USE_DECODE_UNORM8 = 1 << 0

    DECOMPRESS_ONLY = 1 << 1
