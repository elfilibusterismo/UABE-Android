cmake_minimum_required(VERSION 3.22.1)
project(t2ddecoder_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(T2D_DIR ${CMAKE_CURRENT_LIST_DIR}/t2d)

file(GLOB_RECURSE T2D_SRC
        ${T2D_DIR}/*.cpp
        ${T2D_DIR}/*.c
)

# EXCLUDE the C++/CLI files (won't build on Android)
list(FILTER T2D_SRC EXCLUDE REGEX ".*Texture2DDecoder\\.cpp$")
list(FILTER T2D_SRC EXCLUDE REGEX ".*AssemblyInfo\\.cpp$")

add_library(t2ddecoder SHARED
        ${T2D_SRC}
        t2d_capi.cpp
)

target_include_directories(t2ddecoder PRIVATE
        ${T2D_DIR}
)

target_compile_options(t2ddecoder PRIVATE -fno-strict-aliasing)

find_library(log-lib log)
target_link_libraries(t2ddecoder ${log-lib})
