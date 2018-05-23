#include "persist.h"


jclass recordCls;
jfieldID typeField;
jfieldID aofTypeField;
jmethodID aqucireMethod;
jfieldID valueField;
jfieldID expireField;

using namespace std;


void initPersist(JNIEnv *env) {
    if (recordCls == NULL) {
        recordCls = (jclass) env->NewGlobalRef(env->FindClass("aredis/NativeRecord"));

        typeField = env->GetFieldID(recordCls, "type", "B");
        aofTypeField = env->GetFieldID(recordCls, "aofType", "B");
        valueField = env->GetFieldID(recordCls, "value", "[B");
        expireField = env->GetFieldID(recordCls, "expire", "J");
        aqucireMethod = env->GetStaticMethodID(recordCls, "acquire", "()Laredis/NativeRecord;");
    }
}

void Java_aredis_Native_set(JNIEnv *env, jclass cls, jstring cacheName, jint ptr, jstring key,
                            jbyte type,
                            jbyteArray value, jlong expire) {

    lru *instance = reinterpret_cast<lru *>(ptr);

    if (instance) {

        char c_type = type;
        int length = env->GetArrayLength(value);
        jbyte *jbarray = (jbyte *) malloc(length * sizeof(jbyte));

        env->GetByteArrayRegion(value, 0, length, jbarray);
        Value *val = new Value();

        val->length = length;
        val->type = c_type;
        val->val = jbarray;
        val->expire = expire;

        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);
        instance->put(mapKey, val);
        pthread_mutex_unlock(&mutex_t);
        env->ReleaseStringUTFChars(key, str);

    } else {
        LOGD("SET NATIVE CACHE NULL: %p", instance);
    }

}

jobject Java_aredis_Native_ladd(JNIEnv *env, jclass cls, jstring cacheName, jint ptr, jstring key,
                                jbyte type,
                                jbyteArray value) {

    lru *instance = reinterpret_cast<lru *>(ptr);
    jobject result;

    if (instance) {
        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);
        int length = env->GetArrayLength(value);
        jbyte *jbarray = (jbyte *) malloc(length * sizeof(jbyte));
        env->GetByteArrayRegion(value, 0, length, jbarray);
        if (instance->ladd(mapKey, type, jbarray, length)) {
            result = env->CallStaticObjectMethod(recordCls, aqucireMethod);

            env->SetByteField(result, typeField, type);
            env->SetByteField(result, aofTypeField, AOF_LADD);
            env->SetObjectField(result, valueField, value);
        }

        pthread_mutex_unlock(&mutex_t);
        env->ReleaseStringUTFChars(key, str);
    } else {
        LOGD("SET NATIVE CACHE NULL:  %p", instance);
    }

    return result;
}

jobject Java_aredis_Native_get(JNIEnv *env, jclass cls, jstring cacheName, jint ptr, jstring key) {

    lru *instance = reinterpret_cast<lru *>(ptr);

    if (instance) {
        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);

        Value *val = instance->get(mapKey);
        pthread_mutex_unlock(&mutex_t);
        if (val) {
            jobject record = env->CallStaticObjectMethod(recordCls, aqucireMethod);

            env->SetByteField(record, typeField, val->type);
            jbyteArray arr = env->NewByteArray(val->length);
            env->SetByteArrayRegion(arr, 0, val->length, val->val);
            env->SetObjectField(record, valueField, arr);
            return record;
        }
        env->ReleaseStringUTFChars(key, str);
    }

    return NULL;
}

JNIEXPORT void JNICALL Java_aredis_Native_remove(JNIEnv *env, jclass cls, jstring cacheName,
                                                 jint ptr,
                                                 jstring key) {

    lru *instance = reinterpret_cast<lru *>(ptr);
    if (instance) {
        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);
        instance->remove(mapKey);
        pthread_mutex_unlock(&mutex_t);
        env->ReleaseStringUTFChars(key, str);
    }
}
