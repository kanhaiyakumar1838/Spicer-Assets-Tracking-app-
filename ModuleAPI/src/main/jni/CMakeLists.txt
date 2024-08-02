cmake_minimum_required (VERSION 3.4.1)
project(ModuleAPI)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/inc")
file(GLOB SRC_LIST "${CMAKE_CURRENT_SOURCE_DIR}/src/*.c"
        )
add_library(
ModuleAPI
SHARED
        ${SRC_LIST}
)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ModuleAPI

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )
