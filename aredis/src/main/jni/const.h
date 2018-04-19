//
// Created by tianyang on 17/11/23.
//
#include <jni.h>
#include <hash_map>
#include <string>
#include <malloc.h>
#include <time.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <list>
#include <vector>
#include <android/log.h>


#define  LOG_TAG    "aredis"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace std;


#ifndef V_TAG
#define V_TAG

struct Value {
    int length = 0;
    jlong expire;
    jbyte type;
    jbyte *val;
    string key;
    Value *prev, *next;
};

struct Length {
    int length;
    jbyte *bytes;
};
extern int MAX_KV_PAIRS;
extern int MAX_MEM_SIZE;
extern pthread_mutex_t mutex_t;
extern int MAX_KEY_VALUE_LENGTH;

extern int AOF_SET;
extern int AOF_DELETE;
extern int AOF_LADD;

extern Length *makeSingleLength( int length);
#endif




