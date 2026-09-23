#include <jni.h>

#include <android/log.h>

#include <GLES2/gl2.h>

#include <dirent.h>

#include <algorithm>
#include <mutex>
#include <string>
#include <vector>

// Public C API of libprojectM. The prebuilt libprojectM-4.so only exports these
// C symbols (projectm_*); the C++ class API has hidden visibility and must not
// be used here, otherwise linking fails with undefined symbols.
#ifdef HAVE_PROJECTM
#include <projectM-4/projectM.h>
#endif

#define LOG_TAG "ProjectMBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Suffix identifying Milkdrop preset files in the preset directory.
static constexpr const char* PRESET_FILE_SUFFIX = ".milk";

static std::mutex g_mutex;
static int g_viewportWidth = 800;
static int g_viewportHeight = 600;

#include <dlfcn.h>
#include <EGL/egl.h>

/**
 * Custom GL proc loader for Android EGL.
 *
 * On Android, eglGetProcAddress loads extension functions but returns NULL for
 * core OpenGL ES 2.0/3.0 functions. This loader tries eglGetProcAddress first and
 * falls back to dlsym on libGLESv3.so / libGLESv2.so for core functions.
 */
static void* android_gl_load_proc(const char* name, void* /* user_data */) {
    if (name == nullptr) {
        return nullptr;
    }
    void* proc = reinterpret_cast<void*>(eglGetProcAddress(name));
    if (proc != nullptr) {
        return proc;
    }
    static void* glesHandle = nullptr;
    if (glesHandle == nullptr) {
        glesHandle = dlopen("libGLESv3.so", RTLD_NOW | RTLD_GLOBAL);
        if (glesHandle == nullptr) {
            glesHandle = dlopen("libGLESv2.so", RTLD_NOW | RTLD_GLOBAL);
        }
    }
    if (glesHandle != nullptr) {
        proc = dlsym(glesHandle, name);
    }
    return proc;
}

#ifdef HAVE_PROJECTM
static void projectm_log_func(const char* message, projectm_log_level /* log_level */, void* /* user_data */) {
    if (message != nullptr) {
        LOGE("[projectM-Core] %s", message);
    }
}

// Opaque projectM instance, created on the GL thread in nativeInit.
static projectm_handle g_projectM = nullptr;
// Absolute path of the directory holding the .milk preset files.
static std::string g_presetDir;
// Sorted preset file names (without directory) for next/previous navigation.
static std::vector<std::string> g_presets;
static size_t g_presetIndex = 0;

/**
 * Scans the preset directory for Milkdrop preset files.
 *
 * Fills g_presets with the sorted file names and resets g_presetIndex.
 * Must be called with g_mutex held.
 */
static void scanPresetDirLocked() {
    g_presets.clear();
    g_presetIndex = 0;
    DIR* dir = opendir(g_presetDir.c_str());
    if (dir == nullptr) {
        LOGE("Cannot open preset directory: %s", g_presetDir.c_str());
        return;
    }
    const std::string suffix = PRESET_FILE_SUFFIX;
    dirent* entry = nullptr;
    while ((entry = readdir(dir)) != nullptr) {
        std::string name = entry->d_name;
        if (name.size() > suffix.size() &&
            name.compare(name.size() - suffix.size(), suffix.size(), suffix) == 0) {
            g_presets.push_back(name);
        }
    }
    closedir(dir);
    std::sort(g_presets.begin(), g_presets.end());
    LOGI("Found %zu presets in %s", g_presets.size(), g_presetDir.c_str());
}

/**
 * Loads the preset at the given index with a smooth transition.
 *
 * Must be called with g_mutex held and a valid g_projectM instance.
 */
static void loadPresetLocked(size_t index) {
    if (g_projectM == nullptr || g_presets.empty()) {
        return;
    }
    g_presetIndex = index % g_presets.size();
    const std::string path = g_presetDir + "/" + g_presets[g_presetIndex];
    projectm_load_preset_file(g_projectM, path.c_str(), true);
    LOGI("Loading preset: %s", path.c_str());
}
#endif

extern "C" {

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeInit(
        JNIEnv* env,
        jobject /* this */,
        jstring presetDirPath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const char* pathStr = env->GetStringUTFChars(presetDirPath, nullptr);
    // Copy the path before releasing the JNI string.
    const std::string presetDir = pathStr != nullptr ? pathStr : "";
    LOGI("nativeInit called with preset dir: %s", presetDir.c_str());

#ifdef HAVE_PROJECTM
    g_presetDir = presetDir;
    scanPresetDirLocked();
    if (g_projectM == nullptr) {
        const char* glVersion = reinterpret_cast<const char*>(glGetString(GL_VERSION));
        const char* glVendor = reinterpret_cast<const char*>(glGetString(GL_VENDOR));
        const char* glRenderer = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
        LOGI("GL Context Info - Version: %s, Vendor: %s, Renderer: %s",
             glVersion ? glVersion : "NULL",
             glVendor ? glVendor : "NULL",
             glRenderer ? glRenderer : "NULL");

        // Register log callback to capture core projectM initialization errors.
        projectm_set_log_callback(projectm_log_func, false, nullptr);
        projectm_set_log_level(PROJECTM_LOG_LEVEL_DEBUG, false);

        // Must run on the GL thread with a current EGL context (called from
        // GLSurfaceView.Renderer.onSurfaceCreated).
        g_projectM = projectm_create_with_opengl_load_proc(android_gl_load_proc, nullptr);
        if (g_projectM == nullptr) {
            LOGE("projectm_create_with_opengl_load_proc failed: no current GL context or GLES initialization error");
        } else {
            projectm_set_window_size(
                    g_projectM,
                    static_cast<size_t>(g_viewportWidth),
                    static_cast<size_t>(g_viewportHeight));
            const char* texturePaths[] = {g_presetDir.c_str()};
            projectm_set_texture_search_paths(g_projectM, texturePaths, 1);
            if (!g_presets.empty()) {
                // Load the first preset immediately without transition.
                const std::string path = g_presetDir + "/" + g_presets.front();
                projectm_load_preset_file(g_projectM, path.c_str(), false);
                LOGI("projectM instance initialized with preset: %s", path.c_str());
            } else {
                LOGI("projectM instance initialized without presets (idle preset active)");
            }
        }
    }
#else
    LOGI("projectM library not present in build; running in stub mode");
#endif

    if (pathStr != nullptr) {
        env->ReleaseStringUTFChars(presetDirPath, pathStr);
    }
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeResize(
        JNIEnv* /* env */,
        jobject /* this */,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_viewportWidth = width;
    g_viewportHeight = height;
    glViewport(0, 0, width, height);

#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr) {
        projectm_set_window_size(
                g_projectM, static_cast<size_t>(width), static_cast<size_t>(height));
    }
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeRender(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);

#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr) {
        projectm_opengl_render_frame(g_projectM);
        return;
    }
#endif

    // Fallback stub background rendering if projectM is not available.
    glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeAddPcm(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray pcmData,
        jint channels) {
    if (pcmData == nullptr) {
        return;
    }

    jsize length = env->GetArrayLength(pcmData);
    if (length == 0) {
        return;
    }

    jfloat* samples = env->GetFloatArrayElements(pcmData, nullptr);

#ifdef HAVE_PROJECTM
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_projectM != nullptr && samples != nullptr) {
        // The C API expects interleaved LRLR samples; count is per channel.
        const unsigned int channelCount = channels >= 2 ? PROJECTM_STEREO : PROJECTM_MONO;
        const unsigned int count =
                static_cast<unsigned int>(length) / channelCount;
        if (count > 0) {
            projectm_pcm_add_float(
                    g_projectM, samples, count,
                    static_cast<projectm_channels>(channelCount));
        }
    }
#endif

    env->ReleaseFloatArrayElements(pcmData, samples, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeNextPreset(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr && !g_presets.empty()) {
        loadPresetLocked(g_presetIndex + 1);
    }
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativePreviousPreset(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr && !g_presets.empty()) {
        loadPresetLocked(g_presetIndex + g_presets.size() - 1);
    }
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeSelectPreset(
        JNIEnv* env,
        jobject /* this */,
        jstring presetName) {
    if (presetName == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef HAVE_PROJECTM
    const char* nameStr = env->GetStringUTFChars(presetName, nullptr);
    if (nameStr != nullptr && g_projectM != nullptr) {
        std::string name(nameStr);
        const std::string path = g_presetDir + "/" + name;
        projectm_load_preset_file(g_projectM, path.c_str(), true);
        LOGI("Selecting preset by name: %s", path.c_str());

        // Update g_presetIndex if name is in g_presets list
        auto it = std::find(g_presets.begin(), g_presets.end(), name);
        if (it != g_presets.end()) {
            g_presetIndex = std::distance(g_presets.begin(), it);
        }
    }
    if (nameStr != nullptr) {
        env->ReleaseStringUTFChars(presetName, nameStr);
    }
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeIsActive(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef HAVE_PROJECTM
    return g_projectM != nullptr ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeRelease(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_mutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr) {
        projectm_destroy(g_projectM);
        g_projectM = nullptr;
        g_presets.clear();
        g_presetIndex = 0;
        LOGI("projectM instance released");
    }
#endif
}

} // extern "C"
