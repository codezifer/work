#include <jni.h>

#include <android/log.h>

#include <GLES2/gl2.h>

#include <dirent.h>
#include <sys/stat.h>

#include <algorithm>
#include <atomic>
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

// Locking model (fixed order: g_stateMutex -> g_pcmMutex, never the reverse):
// - g_stateMutex guards the projectM instance, preset list, viewport size and
//   init/release. It is held during projectm_opengl_render_frame.
// - g_pcmMutex guards only the pending PCM queue. nativeAddPcm appends samples
//   under this short lock and never touches the projectM instance, so the audio
//   thread never blocks on a render frame. nativeRender drains the queue under
//   both locks (state already held) and feeds the drained copy to projectM.
// - g_instanceActive is atomic and read without any lock (see nativeIsActive).
static std::mutex g_stateMutex;
static std::mutex g_pcmMutex;
static std::atomic<bool> g_instanceActive{false};
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
    // Resolved once; the handle itself is only read afterwards.
    static void* glesHandle = nullptr;
    static std::once_flag glesHandleOnce;
    std::call_once(glesHandleOnce, [] {
        glesHandle = dlopen("libGLESv3.so", RTLD_NOW | RTLD_GLOBAL);
        if (glesHandle == nullptr) {
            glesHandle = dlopen("libGLESv2.so", RTLD_NOW | RTLD_GLOBAL);
        }
    });
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
// Whether any preset was ever loaded into the instance. A smooth transition
// needs a valid source preset; the first load must always be hard (smooth=false),
// otherwise the transition renderer dereferences null GL state and crashes.
static bool g_hasPresetLoaded = false;
// Last requested preset name whose file was missing (extraction still pending).
// Retried on the next re-init after extraction completes; latest request wins.
static std::string g_pendingPreset;

/**
 * Whether a GL context is current on this thread. GL calls without one fail
 * (and projectM creation/preset loads misbehave), so context-less invocations
 * from lifecycle races must skip GL work or defer it.
 */
static bool hasCurrentGlContext() {
    return glGetString(GL_VERSION) != nullptr;
}

// Pending PCM samples (interleaved LRLR floats) queued by the audio thread and
// drained by the render thread. Bounded so a stalled renderer cannot grow memory
// without limit; oldest samples are dropped on overflow.
static std::vector<float> g_pendingPcm;
static int g_pendingPcmChannels = 2;
static constexpr size_t kMaxPendingPcmSamples = 16384;

/**
 * Scans the preset directory for Milkdrop preset files.
 *
 * Fills g_presets with the sorted file names and resets g_presetIndex.
 * Must be called with g_stateMutex held.
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
 * Loads the preset at the given index.
 *
 * The first preset of an instance must load with smooth=false: a smooth
 * transition needs a valid source preset, and transitioning out of the blank
 * idle state crashes inside libprojectM (null VAO/program in glDrawElements).
 *
 * Must be called with g_stateMutex held and a valid g_projectM instance.
 */
static void loadPresetLocked(size_t index, bool smooth) {
    if (g_projectM == nullptr || g_presets.empty()) {
        return;
    }
    if (!g_hasPresetLoaded) {
        smooth = false;
    }
    g_presetIndex = index % g_presets.size();
    const std::string path = g_presetDir + "/" + g_presets[g_presetIndex];
    projectm_load_preset_file(g_projectM, path.c_str(), smooth);
    g_hasPresetLoaded = true;
    LOGI("Loading preset: %s", path.c_str());
}
#endif

extern "C" {

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeInit(
        JNIEnv* env,
        jobject /* this */,
        jstring presetDirPath) {
    std::lock_guard<std::mutex> lock(g_stateMutex);
    const char* pathStr = env->GetStringUTFChars(presetDirPath, nullptr);
    // Copy the path before releasing the JNI string.
    const std::string presetDir = pathStr != nullptr ? pathStr : "";
    LOGI("nativeInit called with preset dir: %s", presetDir.c_str());

#ifdef HAVE_PROJECTM
    g_presetDir = presetDir;
    scanPresetDirLocked();
    const bool haveContext = hasCurrentGlContext();
    if (g_projectM == nullptr) {
        if (!haveContext) {
            // Lifecycle race (e.g. queued init before the first surface exists):
            // creation without a current context fails; onSurfaceCreated retries.
            LOGE("nativeInit without current GL context, deferring instance creation");
        } else {
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
                g_instanceActive.store(false, std::memory_order_release);
            } else {
                g_instanceActive.store(true, std::memory_order_release);
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
                    g_hasPresetLoaded = true;
                    LOGI("projectM instance initialized with preset: %s", path.c_str());
                } else {
                    LOGI("projectM instance initialized without presets (idle preset active)");
                }
            }
        }
    } else {
        if (!haveContext) {
            // No GL work without a context; a later onSurfaceCreated re-init settles state.
            LOGE("nativeInit re-scan without current GL context, deferring preset settle");
        } else if (!g_pendingPreset.empty()) {
            // Re-init (e.g. after async preset extraction finished): only rescan,
            // then settle pending state. A first load is always hard so later user
            // switches transition from a valid source preset.
            const std::string pendingPath = g_presetDir + "/" + g_pendingPreset;
            struct stat st;
            if (stat(pendingPath.c_str(), &st) == 0) {
                projectm_load_preset_file(g_projectM, pendingPath.c_str(), g_hasPresetLoaded);
                g_hasPresetLoaded = true;
                auto it = std::find(g_presets.begin(), g_presets.end(), g_pendingPreset);
                if (it != g_presets.end()) {
                    g_presetIndex = static_cast<size_t>(std::distance(g_presets.begin(), it));
                }
                LOGI("Loading pending preset: %s", pendingPath.c_str());
                g_pendingPreset.clear();
            }
        } else if (!g_hasPresetLoaded && !g_presets.empty()) {
            loadPresetLocked(0, false);
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
    std::lock_guard<std::mutex> lock(g_stateMutex);
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
    std::lock_guard<std::mutex> stateLock(g_stateMutex);

#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr) {
        // Drain the audio-thread PCM queue (brief pcm lock, fixed order) and
        // feed the drained copy to projectM on the render thread.
        std::vector<float> drained;
        int drainedChannels = 2;
        {
            std::lock_guard<std::mutex> pcmLock(g_pcmMutex);
            if (!g_pendingPcm.empty()) {
                drained.swap(g_pendingPcm);
                drainedChannels = g_pendingPcmChannels;
            }
        }
        if (!drained.empty()) {
            const unsigned int channelCount =
                    drainedChannels >= 2 ? PROJECTM_STEREO : PROJECTM_MONO;
            const unsigned int count =
                    static_cast<unsigned int>(drained.size()) / channelCount;
            if (count > 0) {
                projectm_pcm_add_float(
                        g_projectM, drained.data(), count,
                        static_cast<projectm_channels>(channelCount));
            }
        }
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
    if (samples == nullptr) {
        return;
    }

#ifdef HAVE_PROJECTM
    // Audio thread: enqueue only, under the short pcm lock. Never touches the
    // projectM instance, so this never blocks on a render frame.
    {
        std::lock_guard<std::mutex> pcmLock(g_pcmMutex);
        const size_t incoming = static_cast<size_t>(length);
        if (g_pendingPcm.size() + incoming > kMaxPendingPcmSamples) {
            const size_t excess = g_pendingPcm.size() + incoming - kMaxPendingPcmSamples;
            // Drop oldest samples to make room for the newest audio.
            g_pendingPcm.erase(
                    g_pendingPcm.begin(),
                    g_pendingPcm.begin() + static_cast<ptrdiff_t>(std::min(excess, g_pendingPcm.size())));
        }
        g_pendingPcmChannels = channels >= 2 ? 2 : 1;
        g_pendingPcm.insert(g_pendingPcm.end(), samples, samples + length);
    }
#endif

    env->ReleaseFloatArrayElements(pcmData, samples, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeNextPreset(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_stateMutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr && !g_presets.empty() && hasCurrentGlContext()) {
        loadPresetLocked(g_presetIndex + 1, true);
    }
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativePreviousPreset(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_stateMutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr && !g_presets.empty() && hasCurrentGlContext()) {
        loadPresetLocked(g_presetIndex + g_presets.size() - 1, true);
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
    std::lock_guard<std::mutex> lock(g_stateMutex);
#ifdef HAVE_PROJECTM
    const char* nameStr = env->GetStringUTFChars(presetName, nullptr);
    if (nameStr != nullptr && g_projectM != nullptr) {
        std::string name(nameStr);
        // Remember the request: if the file is missing (extraction pending),
        // the next re-init retries it instead of losing the selection.
        g_pendingPreset = name;
        if (!hasCurrentGlContext()) {
            LOGE("selectPreset without current GL context, deferring: %s", name.c_str());
        } else {
        const std::string path = g_presetDir + "/" + name;
        struct stat st;
        if (stat(path.c_str(), &st) != 0) {
            LOGE("Preset file not found, deferring until extraction completes: %s", path.c_str());
        } else {
            // First load is hard: a smooth transition out of the blank idle state
            // crashes inside libprojectM (see loadPresetLocked).
            const bool smooth = g_hasPresetLoaded;
            projectm_load_preset_file(g_projectM, path.c_str(), smooth);
            g_hasPresetLoaded = true;
            g_pendingPreset.clear();
            LOGI("Selecting preset by name: %s", path.c_str());

            // Update g_presetIndex if name is in g_presets list
            auto it = std::find(g_presets.begin(), g_presets.end(), name);
            if (it != g_presets.end()) {
                g_presetIndex = static_cast<size_t>(std::distance(g_presets.begin(), it));
            }
        }
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
    // Lock-free: the audio thread queries this per buffer and must never block
    // on a render frame. The flag is set on init and cleared on release.
#ifdef HAVE_PROJECTM
    return g_instanceActive.load(std::memory_order_acquire) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeClearPcm(
        JNIEnv* /* env */,
        jobject /* this */) {
    // Short pcm lock only; never touches the instance, safe from any thread.
    std::lock_guard<std::mutex> pcmLock(g_pcmMutex);
    g_pendingPcm.clear();
}

JNIEXPORT void JNICALL
Java_de_carsten_android_muzzic_visualization_projectm_ProjectMNativeBridge_nativeRelease(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> stateLock(g_stateMutex);
#ifdef HAVE_PROJECTM
    if (g_projectM != nullptr) {
        projectm_destroy(g_projectM);
        g_projectM = nullptr;
        g_instanceActive.store(false, std::memory_order_release);
        g_presets.clear();
        g_presetIndex = 0;
        g_hasPresetLoaded = false;
        g_pendingPreset.clear();
        LOGI("projectM instance released");
    }
    {
        std::lock_guard<std::mutex> pcmLock(g_pcmMutex);
        g_pendingPcm.clear();
    }
#endif
}

} // extern "C"
