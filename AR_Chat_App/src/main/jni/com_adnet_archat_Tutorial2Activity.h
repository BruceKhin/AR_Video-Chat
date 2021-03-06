/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>

#ifndef com_adnet_archat_Tutorial2Activity
#define com_adnet_archat_Tutorial2Activity

#ifdef __cplusplus
extern "C" {
#endif
#undef com_adnet_archat_Tutorial2Activity_BIND_ABOVE_CLIENT
#define com_adnet_archat_Tutorial2Activity_BIND_ABOVE_CLIENT 8L
#undef com_adnet_archat_Tutorial2Activity_BIND_ADJUST_WITH_ACTIVITY
#define com_adnet_archat_Tutorial2Activity_BIND_ADJUST_WITH_ACTIVITY 128L
#undef com_adnet_archat_Tutorial2Activity_BIND_ALLOW_OOM_MANAGEMENT
#define com_adnet_archat_Tutorial2Activity_BIND_ALLOW_OOM_MANAGEMENT 16L
#undef com_adnet_archat_Tutorial2Activity_BIND_AUTO_CREATE
#define com_adnet_archat_Tutorial2Activity_BIND_AUTO_CREATE 1L
#undef com_adnet_archat_Tutorial2Activity_BIND_DEBUG_UNBIND
#define com_adnet_archat_Tutorial2Activity_BIND_DEBUG_UNBIND 2L
#undef com_adnet_archat_Tutorial2Activity_BIND_IMPORTANT
#define com_adnet_archat_Tutorial2Activity_BIND_IMPORTANT 64L
#undef com_adnet_archat_Tutorial2Activity_BIND_NOT_FOREGROUND
#define com_adnet_archat_Tutorial2Activity_BIND_NOT_FOREGROUND 4L
#undef com_adnet_archat_Tutorial2Activity_BIND_WAIVE_PRIORITY
#define com_adnet_archat_Tutorial2Activity_BIND_WAIVE_PRIORITY 32L
#undef com_adnet_archat_Tutorial2Activity_CONTEXT_IGNORE_SECURITY
#define com_adnet_archat_Tutorial2Activity_CONTEXT_IGNORE_SECURITY 2L
#undef com_adnet_archat_Tutorial2Activity_CONTEXT_INCLUDE_CODE
#define com_adnet_archat_Tutorial2Activity_CONTEXT_INCLUDE_CODE 1L
#undef com_adnet_archat_Tutorial2Activity_CONTEXT_RESTRICTED
#define com_adnet_archat_Tutorial2Activity_CONTEXT_RESTRICTED 4L
#undef com_adnet_archat_Tutorial2Activity_MODE_APPEND
#define com_adnet_archat_Tutorial2Activity_MODE_APPEND 32768L
#undef com_adnet_archat_Tutorial2Activity_MODE_ENABLE_WRITE_AHEAD_LOGGING
#define com_adnet_archat_Tutorial2Activity_MODE_ENABLE_WRITE_AHEAD_LOGGING 8L
#undef com_adnet_archat_Tutorial2Activity_MODE_MULTI_PROCESS
#define com_adnet_archat_Tutorial2Activity_MODE_MULTI_PROCESS 4L
#undef com_adnet_archat_Tutorial2Activity_MODE_PRIVATE
#define com_adnet_archat_Tutorial2Activity_MODE_PRIVATE 0L
#undef com_adnet_archat_Tutorial2Activity_MODE_WORLD_READABLE
#define com_adnet_archat_Tutorial2Activity_MODE_WORLD_READABLE 1L
#undef com_adnet_archat_Tutorial2Activity_MODE_WORLD_WRITEABLE
#define com_adnet_archat_Tutorial2Activity_MODE_WORLD_WRITEABLE 2L
#undef com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_DIALER
#define com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_DIALER 1L
#undef com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_DISABLE
#define com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_DISABLE 0L
#undef com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SEARCH_GLOBAL
#define com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SEARCH_GLOBAL 4L
#undef com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SEARCH_LOCAL
#define com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SEARCH_LOCAL 3L
#undef com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SHORTCUT
#define com_adnet_archat_Tutorial2Activity_DEFAULT_KEYS_SHORTCUT 2L
#undef com_adnet_archat_Tutorial2Activity_RESULT_CANCELED
#define com_adnet_archat_Tutorial2Activity_RESULT_CANCELED 0L
#undef com_adnet_archat_Tutorial2Activity_RESULT_FIRST_USER
#define com_adnet_archat_Tutorial2Activity_RESULT_FIRST_USER 1L
#undef com_adnet_archat_Tutorial2Activity_RESULT_OK
#define com_adnet_archat_Tutorial2Activity_RESULT_OK -1L

/*
 * Class:     com_adnet_archat_Tutorial2Activity
 * Method:    predict
 * Signature: (Landroid/graphics/Bitmap;[B)V
 */


JNIEXPORT jintArray JNICALL
        Java_com_adnet_archat_Tutorial2Activity_CMTgetRect(JNIEnv *env, jobject instance);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_CMTLoad(JNIEnv *env, jobject instance,
                                                      jstring Path_);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_CMTSave(JNIEnv *env, jobject instance,
                                                      jstring Path_);
JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_TLDSave(JNIEnv *env, jobject instance,
                                                      jstring Path_);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_TLDLoad(JNIEnv *env, jobject instance,
                                                      jstring Path_);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_ProcessCMT(JNIEnv *env, jobject instance,
                                                         jlong matAddrGr, jlong matAddrRgba);
JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_OpenCMT(JNIEnv *env, jobject instance,
                                                      jlong matAddrGr, jlong matAddrRgba, jlong x,
                                                      jlong y, jlong w, jlong h);

JNIEXPORT jintArray JNICALL
        Java_com_adnet_archat_Tutorial2Activity_getRect(JNIEnv *env, jobject instance);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_ProcessTLD(JNIEnv *env, jobject instance,
                                                         jlong matAddrGr, jlong matAddrRgba);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_OpenTLD(JNIEnv *env, jobject instance,
                                                      jlong matAddrGr, jlong matAddrRgba, jlong x,
                                                      jlong y, jlong w, jlong h);

JNIEXPORT void JNICALL
Java_com_adnet_archat_Tutorial2Activity_FindFeatures(JNIEnv *env, jobject instance,
                                                           jlong matAddrGr, jlong matAddrRgba);

#ifdef __cplusplus
}
#endif
#endif
