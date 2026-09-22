#!/usr/bin/env bash
set -e

# Script to clone and cross-compile libprojectM for Android ABIs using NDK.
# Must be run from the visualization/ module directory.
#
# The resulting binaries (src/main/jniLibs/<abi>/libprojectM-4.so) only export
# the public C API (projectm_*). The JNI bridge (src/main/cpp) therefore uses
# <projectM-4/projectM.h> and must NOT use the C++ class API.

if [ ! -f "build.gradle.kts" ] || [ ! -d "src/main/cpp" ]; then
    echo "Error: run this script from the visualization/ module directory."
    exit 1
fi

SDK_PATH="${ANDROID_HOME:-/opt/android/sdk}"
NDK_VERSION="${NDK_VERSION:-30.0.16248370}"
NDK_PATH="${ANDROID_NDK_HOME:-$SDK_PATH/ndk/$NDK_VERSION}"
TOOLCHAIN="${NDK_PATH}/build/cmake/android.toolchain.cmake"
# Matches appMinSdk in gradle/libs.versions.toml; prebuilts must not target
# a newer platform than the module's minSdk.
ANDROID_PLATFORM="${ANDROID_PLATFORM:-android-36}"
LLVM_STRIP="${NDK_PATH}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

if [ ! -f "${TOOLCHAIN}" ]; then
    echo "Error: Android NDK CMake toolchain not found at ${TOOLCHAIN}"
    echo "Please set ANDROID_HOME or ANDROID_NDK_HOME environment variable."
    exit 1
fi

WORK_DIR="/tmp/projectm_build"
SRC_DIR="${WORK_DIR}/projectm_src"

echo "=== Cloning projectM repository with submodules (master branch) ==="
rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"
git clone --recurse-submodules --depth 1 https://github.com/projectM-visualizer/projectm.git "${SRC_DIR}"
git -C "${SRC_DIR}" rev-parse HEAD > "${WORK_DIR}/upstream-commit.txt"
cat "${WORK_DIR}/upstream-commit.txt"

ABIS=("arm64-v8a" "x86_64" "armeabi-v7a" "x86")

for ABI in "${ABIS[@]}"; do
    echo "=== Building libprojectM for ${ABI} ==="
    BUILD_DIR="${WORK_DIR}/build-${ABI}"

    cmake -B "${BUILD_DIR}" -S "${SRC_DIR}" \
        -DCMAKE_TOOLCHAIN_FILE="${TOOLCHAIN}" \
        -DANDROID_ABI="${ABI}" \
        -DANDROID_PLATFORM="${ANDROID_PLATFORM}" \
        -DENABLE_GLES=ON \
        -DENABLE_SDL=OFF \
        -DENABLE_QT=OFF \
        -DBUILD_TESTING=OFF \
        -DCMAKE_BUILD_TYPE=Release

    cmake --build "${BUILD_DIR}" --target projectM -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

    DEST_JNI_DIR="src/main/jniLibs/${ABI}"
    mkdir -p "${DEST_JNI_DIR}"

    SO_FILE=$(find "${BUILD_DIR}" -maxdepth 4 -name "libprojectM-4.so" | head -n 1)
    if [ -z "${SO_FILE}" ]; then
        echo "Error: libprojectM-4.so not found for ${ABI}"
        exit 1
    fi
    cp "${SO_FILE}" "${DEST_JNI_DIR}/"
    if [ -x "${LLVM_STRIP}" ]; then
        "${LLVM_STRIP}" "${DEST_JNI_DIR}/libprojectM-4.so"
    fi
    echo "Successfully copied libprojectM libraries to ${DEST_JNI_DIR}/"
done

echo "=== Copying C++ header files ==="
DEST_INC="${PWD}/src/main/cpp/include"
mkdir -p "${DEST_INC}"
(cd "${SRC_DIR}/src/libprojectM" && find . \( -name "*.h" -o -name "*.hpp" \) -exec cp --parents {} "${DEST_INC}/" \;)

echo "=== Copying public C API headers ==="
mkdir -p "${DEST_INC}/projectM-4"
cp "${SRC_DIR}/src/api/include/projectM-4/"*.h "${DEST_INC}/projectM-4/"
# version.h and projectM_export.h are generated at configure time; take them
# from the first ABI build directory.
for GENERATED in version.h projectM_export.h; do
    GENERATED_FILE=$(find "${WORK_DIR}/build-arm64-v8a" -path "*projectM-4/${GENERATED}" | head -n 1)
    if [ -n "${GENERATED_FILE}" ]; then
        cp "${GENERATED_FILE}" "${DEST_INC}/projectM-4/${GENERATED}"
    else
        echo "Warning: generated ${GENERATED} not found, keeping existing copy."
    fi
done

echo "=== projectM build complete (upstream: $(cat "${WORK_DIR}/upstream-commit.txt")) ==="
