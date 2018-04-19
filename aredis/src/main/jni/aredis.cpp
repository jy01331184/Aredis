//
// Created by tianyang on 17/9/28.
//

#include <unistd.h>
#include "aredis.h"


void *thread_func(void *arg) {
    sleep(MAX_WAIT_TIME);
    LOGE("OVER TIME EXIT %d", getpid());
    _exit(0);
}


void initAredis(JNIEnv *env) {

}

void Java_aredis_Native_writeAof(JNIEnv *env, jclass cls, jstring cacheName, jstring key,
                                 jobject record) {
    const char *str = env->GetStringUTFChars(cacheName, 0);
    const char *keyStr = env->GetStringUTFChars(key, 0);

    jbyte type = env->GetByteField(record, typeField);
    jbyte aofType = env->GetByteField(record, aofTypeField);
    jbyteArray values = (jbyteArray) env->GetObjectField(record, valueField);
    int length = env->GetArrayLength(values);
    long expire = env->GetLongField(record, expireField);
    jbyte *valueBytes = (jbyte *) malloc(length * sizeof(jbyte));

    env->GetByteArrayRegion(values, 0, length, valueBytes);

    Value *value = new Value();
    value->type = type;
    value->val = valueBytes;
    value->length = length;
    value->expire = expire;

    appendAof(env, str, keyStr, value, aofType);
    env->ReleaseStringUTFChars(cacheName, str);
    env->ReleaseStringUTFChars(key, keyStr);
    free(valueBytes);
    delete value;
}

void Java_aredis_Native_deleteAof(JNIEnv *env, jclass cls, jstring cacheName, jstring key) {
    const char *str = env->GetStringUTFChars(cacheName, 0);
    const char *keyStr = env->GetStringUTFChars(key, 0);
    deleteAof(env, str, keyStr);
    env->ReleaseStringUTFChars(cacheName, str);
    env->ReleaseStringUTFChars(key, keyStr);
}

void Java_aredis_Native_readAof(JNIEnv *env, jclass cls, jstring cacheName) {
    const char *str = env->GetStringUTFChars(cacheName, 0);
    lru *l = readAof(env, str);
    init(str, l);

    env->ReleaseStringUTFChars(cacheName, str);
}

void Java_aredis_Native_syncAof(JNIEnv *env, jclass cls, jstring cacheName) {
    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);
    lru *instance = getCache(dictKey);
    syncAof(env, dictStr, instance->getCache());
    env->ReleaseStringUTFChars(cacheName, dictStr);
}

void Java_aredis_Native_readRdb(JNIEnv *env, jclass cls, jstring cacheName) {
    const char *str = env->GetStringUTFChars(cacheName, 0);
    lru *l = readRdb(env, str);
    init(str, l);
    env->ReleaseStringUTFChars(cacheName, str);
}

void Java_aredis_Native_syncRdb(JNIEnv *env, jclass cls, jstring cacheName) {
    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);
    lru *instance = getCache(dictKey);
    syncRdb(env, dictStr, instance->getCache());
    env->ReleaseStringUTFChars(cacheName, dictStr);
}

JNIEXPORT jint JNICALL Java_aredis_Native_forkNative(JNIEnv *env, jclass cls, jstring cacheName) {
    int fpid;
    fpid = vfork();

    if (fpid < 0)
        LOGE("error in fork!");
    else if (fpid == 0) {
        //pthread_t vpid;
        //pthread_create(&vpid, NULL, thread_func, NULL);
        const char *dictStr = env->GetStringUTFChars(cacheName, 0);
        string dictKey(dictStr);

        lru *instance = getCache(dictKey);
        if (instance) {
            pthread_mutex_lock(&mutex_t);
            instance->startFork();
            pthread_mutex_unlock(&mutex_t);

            syncRdb(env, dictStr, instance->getCache());

            pthread_mutex_lock(&mutex_t);
            //instance->endFork();
            pthread_mutex_unlock(&mutex_t);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                LOGE("Java_aredis_Native_fork error");
            }
        }
        env->ReleaseStringUTFChars(cacheName, dictStr);
        _exit(0);
    }
    else {
    }
    return fpid;
}


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    initAredis(env);
    initPersist(env);
    LOGD("INIT:ARedis.cpp");

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {

    LOGD("UNLOAD:ARedis.cpp");
}
