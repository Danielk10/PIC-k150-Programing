#include <android/fdsan.h>
#include <dlfcn.h>
#include <android/log.h>

#define TAG "FdsanBypass"

__attribute__((constructor)) void disable_fdsan() {
    void *lib_handle = dlopen("libc.so", RTLD_LAZY);
    if (lib_handle) {
        auto set_fdsan_error_level = (void (*)(enum android_fdsan_error_level))
            dlsym(lib_handle, "android_fdsan_set_error_level");
        
        if (set_fdsan_error_level) {
            set_fdsan_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
            __android_log_print(ANDROID_LOG_INFO, TAG, "fdsan disabled successfully in PIC k150");
        } else {
            __android_log_print(ANDROID_LOG_WARN, TAG, "android_fdsan_set_error_level not found in libc");
        }
        dlclose(lib_handle);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Could not load libc.so");
    }
}
