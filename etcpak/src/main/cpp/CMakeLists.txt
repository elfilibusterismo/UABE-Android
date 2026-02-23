cmake_minimum_required(VERSION 3.22.1)
project(native_all LANGUAGES C CXX)

set(PROJ_DIR "${CMAKE_CURRENT_LIST_DIR}")

get_filename_component(REPO_ROOT "${PROJ_DIR}/../../../.." ABSOLUTE)

set(ETCPAK_DIR "${REPO_ROOT}/third_party/etcpak")

if(NOT EXISTS "${ETCPAK_DIR}/Decode.cpp")
    message(FATAL_ERROR
        "ETCPAK not found at: ${ETCPAK_DIR}\n"
        "Fix the REPO_ROOT .. depth, or pass -DREPO_ROOT=... from Gradle."
    )
endif()

add_library(etcpak SHARED
    etcpak_bridge/etcpak_capi.cpp

    ${ETCPAK_DIR}/ColorSpace.cpp
    ${ETCPAK_DIR}/Debug.cpp
    ${ETCPAK_DIR}/Decode.cpp
    ${ETCPAK_DIR}/Dither.cpp
    ${ETCPAK_DIR}/Error.cpp
    ${ETCPAK_DIR}/ProcessDxtc.cpp
    ${ETCPAK_DIR}/ProcessRGB.cpp
    ${ETCPAK_DIR}/System.cpp
    ${ETCPAK_DIR}/Tables.cpp
    ${ETCPAK_DIR}/TaskDispatch.cpp
    ${ETCPAK_DIR}/TextureHeader.cpp
    ${ETCPAK_DIR}/Timing.cpp

    ${ETCPAK_DIR}/bc7enc.cpp
    ${ETCPAK_DIR}/bcdec.c
)

target_include_directories(etcpak PRIVATE
    "${ETCPAK_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/etcpak_bridge"
)

target_compile_features(etcpak PRIVATE cxx_std_17)
target_compile_options(etcpak PRIVATE -O3 -ffast-math)

find_library(log-lib log)
target_link_libraries(etcpak PRIVATE ${log-lib})
