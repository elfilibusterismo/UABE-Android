cmake_minimum_required(VERSION 3.22.1)
project(astc_encoder_android)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(PROJ_DIR "${CMAKE_CURRENT_LIST_DIR}")

get_filename_component(REPO_ROOT "${PROJ_DIR}/../../../.." ABSOLUTE)

set(ASTCENC_DIR "${REPO_ROOT}/third_party/astcenc")
set(ASTC_SRC_DIR "${ASTCENC_DIR}/Source")

if (NOT EXISTS "${ASTC_SRC_DIR}/astcenc_entry.cpp")
    message(FATAL_ERROR "ASTC encoder sources not found at: ${ASTC_SRC_DIR}")
endif()

set(ASTC_SOURCES
    ${ASTC_SRC_DIR}/astcenc_averages_and_directions.cpp
    ${ASTC_SRC_DIR}/astcenc_block_sizes.cpp
    ${ASTC_SRC_DIR}/astcenc_color_quantize.cpp
    ${ASTC_SRC_DIR}/astcenc_color_unquantize.cpp
    ${ASTC_SRC_DIR}/astcenc_compress_symbolic.cpp
    ${ASTC_SRC_DIR}/astcenc_compute_variance.cpp
    ${ASTC_SRC_DIR}/astcenc_decompress_symbolic.cpp
    ${ASTC_SRC_DIR}/astcenc_diagnostic_trace.cpp
    ${ASTC_SRC_DIR}/astcenc_entry.cpp
    ${ASTC_SRC_DIR}/astcenc_find_best_partitioning.cpp
    ${ASTC_SRC_DIR}/astcenc_ideal_endpoints_and_weights.cpp
    ${ASTC_SRC_DIR}/astcenc_image.cpp
    ${ASTC_SRC_DIR}/astcenc_integer_sequence.cpp
    ${ASTC_SRC_DIR}/astcenc_mathlib.cpp
    ${ASTC_SRC_DIR}/astcenc_mathlib_softfloat.cpp
    ${ASTC_SRC_DIR}/astcenc_partition_tables.cpp
    ${ASTC_SRC_DIR}/astcenc_percentile_tables.cpp
    ${ASTC_SRC_DIR}/astcenc_pick_best_endpoint_format.cpp
    ${ASTC_SRC_DIR}/astcenc_quantization.cpp
    ${ASTC_SRC_DIR}/astcenc_symbolic_physical.cpp
    ${ASTC_SRC_DIR}/astcenc_weight_align.cpp
    ${ASTC_SRC_DIR}/astcenc_weight_quant_xfer_tables.cpp
)

add_library(astc_encoder_android SHARED
    astc_android_api.cpp
    ${ASTC_SOURCES}
)

target_include_directories(astc_encoder_android PRIVATE
    ${ASTC_SRC_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(astc_encoder_android PRIVATE ASTCENC_NEON=1)
endif()

target_compile_options(astc_encoder_android PRIVATE -O3)
