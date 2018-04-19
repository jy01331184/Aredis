#include "persist.h"


jclass recordCls;
jfieldID typeField;
jfieldID aofTypeField;
jmethodID aqucireMethod;
jfieldID valueField;
jfieldID expireField;

using namespace std;
hash_map<string, lru *> *dicts;


void initPersist(JNIEnv *env) {
    if (recordCls == NULL) {
        recordCls = (jclass) env->NewGlobalRef(env->FindClass("aredis/NativeRecord"));

        typeField = env->GetFieldID(recordCls, "type", "B");
        aofTypeField = env->GetFieldID(recordCls, "aofType", "B");
        valueField = env->GetFieldID(recordCls, "value", "[B");
        expireField = env->GetFieldID(recordCls, "expire", "J");
        aqucireMethod = env->GetStaticMethodID(recordCls, "acquire", "()Laredis/NativeRecord;");

        dicts = new hash_map<string, lru *>();
    }
}

void Java_aredis_Native_set(JNIEnv *env, jclass cls, jstring cacheName, jstring key, jbyte type,
                            jbyteArray value, jlong expire) {

    char c_type = type;
    int length = env->GetArrayLength(value);
    jbyte *jbarray = (jbyte *) malloc(length * sizeof(jbyte));

    env->GetByteArrayRegion(value, 0, length, jbarray);
    Value *val = new Value();

    val->length = length;
    val->type = c_type;
    val->val = jbarray;
    val->expire = expire;

    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);

    lru *instance = dicts->operator[](dictKey);
    if (instance) {
        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);
        instance->put(mapKey, val);
        pthread_mutex_unlock(&mutex_t);
        env->ReleaseStringUTFChars(key, str);
    } else {
        LOGD("SET NATIVE CACHE NULL: %s %p", dictStr, instance);
    }
    env->ReleaseStringUTFChars(cacheName, dictStr);

    //LOGD("SET NATIVE CACHE : %s %p", dictStr,instance);
}

jobject Java_aredis_Native_ladd(JNIEnv *env, jclass cls, jstring cacheName, jstring key, jbyte type,
                                jbyteArray value) {
    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);
    jobject result;
    lru *instance = dicts->operator[](dictKey);
    //LOGD("GET NATIVE CACHE : %s %p %p", dictStr,instance,&dicts);
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
        LOGD("SET NATIVE CACHE NULL: %s %p", dictStr, instance);
    }
    env->ReleaseStringUTFChars(cacheName, dictStr);

    return result;
    //LOGD("SET NATIVE CACHE : %s %p", dictStr,instance);
}


int recurse_madness(int level) {
    static int var[] = {1, 2};
    if (level > 2000) {
        return 1 + level;
    } else {
        return recurse_madness(level + 1) * var[level];
    }
}

jobject Java_aredis_Native_get(JNIEnv *env, jclass cls, jstring cacheName, jstring key) {

    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);

    lru *instance = dicts->operator[](dictKey);
    //LOGD("GET NATIVE CACHE : %s %p %p", dictStr,instance,&dicts);
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
    env->ReleaseStringUTFChars(cacheName, dictStr);

    return NULL;
}

JNIEXPORT void JNICALL Java_aredis_Native_remove(JNIEnv *env, jclass cls, jstring cacheName,
                                                 jstring key) {
    const char *dictStr = env->GetStringUTFChars(cacheName, 0);
    string dictKey(dictStr);
    lru *instance = dicts->operator[](dictKey);
    if (instance) {
        const char *str = env->GetStringUTFChars(key, 0);
        string mapKey(str);
        pthread_mutex_lock(&mutex_t);
        instance->remove(mapKey);
        pthread_mutex_unlock(&mutex_t);
        env->ReleaseStringUTFChars(key, str);
    }

    env->ReleaseStringUTFChars(cacheName, dictStr);
}

lru *getCache(string cacheName) {
    return dicts->operator[](cacheName);
}


void init(const char *cacheName, lru *l) {
    string dictKey(cacheName);

    if (dicts->count(dictKey) > 0) {
        lru *old = dicts->operator[](dictKey);
        dicts->erase(dictKey);
        LOGD("ALREADY HAVE CACHE %s %d ", cacheName, old->size());
        delete old;
    }

    dicts->operator[](dictKey) = l;
    LOGD("INIT NATIVE CACHE : %s size : %d ", cacheName, l->size());

}