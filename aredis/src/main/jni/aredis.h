//
// Created by tianyang on 17/9/28.
//
#include "const.h"
#include "aio.h"
#include "persist.h"
int MAX_WAIT_TIME = 3;

#ifndef _Included_com_example_wastrel_hellojni_HelloJNI
#define _Included_com_example_wastrel_hellojni_HelloJNI
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_aredis_Native_forkNative(JNIEnv *, jobject , jstring,jint);

JNIEXPORT void JNICALL Java_aredis_Native_syncRdb(JNIEnv *, jobject, jstring,jint);

JNIEXPORT jint JNICALL Java_aredis_Native_readRdb(JNIEnv *, jobject , jstring);

JNIEXPORT void JNICALL Java_aredis_Native_syncAof(JNIEnv *, jobject , jstring,jint);

JNIEXPORT jint JNICALL Java_aredis_Native_readAof(JNIEnv *, jobject , jstring);

JNIEXPORT void JNICALL Java_aredis_Native_writeAof(JNIEnv *, jobject, jstring, jstring, jobject);

JNIEXPORT void JNICALL Java_aredis_Native_deleteAof(JNIEnv *, jobject , jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif

