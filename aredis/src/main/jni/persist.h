//
// Created by tianyang on 17/11/23.
//
#include "const.h"
#include "lru.h"


extern jclass recordCls;
extern jmethodID aqucireMethod;
extern jfieldID typeField;
extern jfieldID aofTypeField;
extern jfieldID valueField;
extern jfieldID expireField;

#ifndef C_TAG
#define C_TAG
#endif

#ifndef _AREDIS_PERSIST
#define _AREDIS_PERSIST
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_aredis_Native_set(JNIEnv *, jobject , jstring,jint , jstring, jbyte, jbyteArray, jlong);

JNIEXPORT jobject JNICALL Java_aredis_Native_ladd(JNIEnv *, jobject , jstring,jint, jstring,jbyte,jbyteArray);

JNIEXPORT jobject JNICALL Java_aredis_Native_get(JNIEnv *, jobject , jstring,jint , jstring);

JNIEXPORT void JNICALL Java_aredis_Native_remove(JNIEnv *, jobject , jstring,jint,jstring);

void initPersist(JNIEnv *env);

void init(const char * cacheName,lru* l);
#ifdef __cplusplus
}
#endif
#endif

