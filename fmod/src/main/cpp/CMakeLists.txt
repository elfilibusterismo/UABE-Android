cmake_minimum_required(VERSION 3.22.1)

add_library(uabe_fmod_toolkit SHARED
        fmod_toolkit/fmod_toolkit_jni.cpp
)

find_library(log-lib log)

get_filename_component(REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../.." ABSOLUTE)

set(FMOD_API_DIR "${REPO_ROOT}/third_party/fmod/api")

target_include_directories(uabe_fmod_toolkit PRIVATE
        ${FMOD_API_DIR}/core/inc
        ${FMOD_API_DIR}/studio/inc
)

add_library(fmod SHARED IMPORTED)
set_target_properties(fmod PROPERTIES
        IMPORTED_NO_SONAME TRUE
        IMPORTED_LOCATION "${FMOD_API_DIR}/core/lib/${ANDROID_ABI}/libfmod.so"
)

add_library(fmodstudio SHARED IMPORTED)
set_target_properties(fmodstudio PROPERTIES
        IMPORTED_NO_SONAME TRUE
        IMPORTED_LOCATION "${FMOD_API_DIR}/studio/lib/${ANDROID_ABI}/libfmodstudio.so"
)

target_link_libraries(uabe_fmod_toolkit
        fmodstudio
        fmod
        ${log-lib}
)
