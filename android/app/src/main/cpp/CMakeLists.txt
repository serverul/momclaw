# MOMCLAW Native Build Configuration
# Minimal CMake setup for future native extensions

cmake_minimum_required(VERSION 3.22.1)
project("momclaw")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for release builds
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_C_FLAGS_RELEASE "-O3 -DNDEBUG")

# ARM64 optimizations (when building for arm64-v8a)
if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64" OR ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -march=armv8.2-a+crypto+dotprod")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -march=armv8.2-a+crypto+dotprod")
endif()

# For now, no native libraries are required.
# The LiteRT-LM and NullClaw integration uses prebuilt binaries.
# This CMakeLists.txt exists for future native extensions (e.g., llama.cpp bindings).

# Placeholder for future native modules:
# add_library(momclaw-native SHARED
#     native_bridge.cpp
# )

# target_link_libraries(momclaw-native
#     android
#     log
# )

# If adding llama.cpp or other native inference in the future:
# add_subdirectory(llama.cpp)
