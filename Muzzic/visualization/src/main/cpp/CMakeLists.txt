cmake_minimum_required(VERSION 3.22.1)
project("projectm_bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

file(GLOB PROJECTM_SO_FILES
    "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libprojectM*.so"
    "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libprojectM*.a"
)

if(PROJECTM_SO_FILES)
    list(GET PROJECTM_SO_FILES 0 PROJECTM_SO)
    add_library(projectM SHARED IMPORTED)
    set_target_properties(projectM PROPERTIES IMPORTED_LOCATION "${PROJECTM_SO}")
    set(PROJECTM_AVAILABLE TRUE)
    message(STATUS "Found projectM library at ${PROJECTM_SO}")
else()
    message(WARNING "libprojectM not found under jniLibs/${ANDROID_ABI}. Building projectm_bridge stub.")
    set(PROJECTM_AVAILABLE FALSE)
endif()

add_library(projectm_bridge SHARED projectm_bridge.cpp)

if(PROJECTM_AVAILABLE)
    target_compile_definitions(projectm_bridge PRIVATE HAVE_PROJECTM=1)
endif()

find_library(log-lib log)
find_library(gles2-lib GLESv2)
find_library(egl-lib EGL)

if(PROJECTM_AVAILABLE)
    target_link_libraries(
        projectm_bridge
        projectM
        ${gles2-lib}
        ${egl-lib}
        ${log-lib}
    )
else()
    target_link_libraries(
        projectm_bridge
        ${gles2-lib}
        ${egl-lib}
        ${log-lib}
    )
endif()
