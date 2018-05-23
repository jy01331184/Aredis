#include <unistd.h>
#include "const.h"
#include "lru.h"
#include <sys/file.h>
#include <sys/stat.h>

const char *HEADER = "AREDIS";
signed int AREDIS_VERSION = 1;
char *EOF_STR = "AREDIS_EOF";
char *RDB_BAK = ".rdb.bak";
char *RDB_POSTFIX = ".rdb";
char *AOF_BAK = ".aof.bak";
char *AOF_POSTFIX = ".aof";

using namespace std;

const int MAX_00 = 127;
const int MAX_10 = 16383;
const int MAX_11 = 4194303;
const int TOP = 63;


char *binary2decimal(int decNum, int digit) {
    char *cs = (char *) malloc(digit);

    for (int i = digit - 1; i >= 0; i--) {
        int index = digit - 1 - i;
        cs[index] = (decNum >> i) & 1;
    }
    return cs;
}

Length *makeSingleLength(int length) {
    Length *len = new Length();

    if (length <= 0) {
        len->length = -1;
    } else if (length <= MAX_00) {
        len->length = 1;
        len->bytes = (jbyte *) malloc(length);
        len->bytes[0] = length;
    } else if (length <= MAX_10) {
        len->length = 2;
        len->bytes = (jbyte *) malloc(length);

        char *total = binary2decimal(length, 14);

        int topChars[8] = {1};
        topChars[0] = 1;
        topChars[1] = 0;
        for (int i = 2; i < 8; i++) {
            topChars[i] = total[i - 2];
        }

        jbyte top = 0;
        for (int i = 0; i < 8; i++) {
            top |= (topChars[i] << (7 - i));
        }

        int bottomChars[8] = {1};
        for (int i = 0; i < 8; i++) {
            bottomChars[i] = total[i + 6];
        }

        jbyte bootom = 0;
        for (int i = 0; i < 8; i++) {
            bootom |= (bottomChars[i] << (7 - i));
        }
        len->bytes[0] = top;
        len->bytes[1] = bootom;

        free(total);
    } else if (length < MAX_11) {
        len->length = 3;
        len->bytes = (jbyte *) malloc(length);

        char *total = binary2decimal(length, 22);

        int topChars[8] = {1};
        topChars[0] = 1;
        topChars[1] = 1;
        for (int i = 2; i < 8; i++) {
            topChars[i] = total[i - 2];
        }

        jbyte top = 0;
        for (int i = 0; i < 8; i++) {
            top |= (topChars[i] << (7 - i));
        }

        int midChars[8] = {1};
        for (int i = 0; i < 8; i++) {
            midChars[i] = total[i + 6];
        }

        jbyte mid = 0;
        for (int i = 0; i < 8; i++) {
            mid |= (midChars[i] << (7 - i));
        }

        int bottomChars[8] = {1};
        for (int i = 0; i < 8; i++) {
            bottomChars[i] = total[i + 14];
        }

        jbyte bootom = 0;
        for (int i = 0; i < 8; i++) {
            bootom |= (bottomChars[i] << (7 - i));
        }
        len->bytes[0] = top;
        len->bytes[1] = mid;
        len->bytes[2] = bootom;
        free(total);
    }


    return len;
}

int getSingleLength(FILE *fileHandle) {
    int result = 0;
    jbyte top = 0;
    fread(&top, sizeof(jbyte), 1, fileHandle);
    if (top < 0) {
        char *topStr = binary2decimal(top & TOP, 8);
        if (top >= -64) {
            jbyte mid = 0;
            jbyte bottom = 0;
            fread(&mid, sizeof(jbyte), 1, fileHandle);
            fread(&bottom, sizeof(jbyte), 1, fileHandle);
            char *midStr = binary2decimal(mid, 8);
            char *bottomStr = binary2decimal(bottom, 8);

            char total[24] = {0};

            memcpy(total, topStr, 8);
            memcpy(total + 8, midStr, 8);
            memcpy(total + 16, bottomStr, 8);

            int length = 0;

            for (int i = 0; i < 24; i++) {
                length |= (total[i] << (23 - i));
            }
            free(bottomStr);
            free(midStr);

            result = length;
        } else {
            jbyte bottom = 0;
            fread(&bottom, sizeof(jbyte), 1, fileHandle);

            char *bottomStr = binary2decimal(bottom, 8);

            char total[16] = {0};

            memcpy(total, topStr, 8);
            memcpy(total + 8, bottomStr, 8);

            int length = 0;

            for (int i = 0; i < 16; i++) {
                length |= (total[i] << (15 - i));
            }
            free(bottomStr);
            result = length;
        }
        free(topStr);
    } else {
        result = top;
    }

    return result;
}

jbyte *longToByte(jlong l) {
    jbyte *bytes = (jbyte *) malloc(8 * sizeof(jbyte));
    bytes[7] = (jbyte) (0xff & l);
    bytes[6] = (jbyte) ((0xff00 & l) >> 8);
    bytes[5] = (jbyte) ((0xff00 & l) >> 16);
    bytes[4] = (jbyte) ((0xff00 & l) >> 24);
    bytes[3] = (jbyte) ((0xff00 & l) >> 32);
    bytes[2] = (jbyte) ((0xff00 & l) >> 40);
    bytes[1] = (jbyte) ((0xff0000 & l) >> 48);
    bytes[0] = (jbyte) ((0xff000000 & l) >> 56);
    return bytes;
}

jbyte *intToByte(int i) {
    jbyte *bytes = (jbyte *) malloc(4 * sizeof(jbyte));

    bytes[3] = (jbyte) (0xff & i);
    bytes[2] = (jbyte) ((0xff00 & i) >> 8);
    bytes[1] = (jbyte) ((0xff0000 & i) >> 16);
    bytes[0] = (jbyte) ((0xff000000 & i) >> 24);
    return bytes;
}

int byteToInt(jbyte *bytes) {
    int i;
    char *pi = (char *) &i;
    pi[0] = bytes[3];
    pi[1] = bytes[2];
    pi[2] = bytes[1];
    pi[3] = bytes[0];
    return i;
}

long byteToLong(jbyte *bytes) {
    long i;
    char *pi = (char *) &i;
    pi[0] = bytes[7];
    pi[1] = bytes[6];
    pi[2] = bytes[5];
    pi[3] = bytes[4];
    pi[4] = bytes[3];
    pi[5] = bytes[2];
    pi[6] = bytes[1];
    pi[7] = bytes[0];
    return i;
}

/**
 * WRITE
 */
void writeHead(FILE *fileHandle) {
    fwrite(HEADER, sizeof(char), strlen(HEADER), fileHandle);
    jbyte *verC = intToByte(AREDIS_VERSION);
    fwrite(verC, sizeof(jbyte), 4, fileHandle);
    free(verC);
}

void writeEof(FILE *fileHandle) {
    fwrite(EOF_STR, sizeof(char), strlen(EOF_STR), fileHandle);
}

int writeType(vector <jbyte> *vec, jbyte type) {
    vec->push_back(type);
    return 1;
}

int writeKey(JNIEnv *env, vector <jbyte> *vec, const char *key) {
    size_t keyLength = strlen(key);
    Length *lengthBytes = makeSingleLength(keyLength);
    if (lengthBytes->length < 0) {
        free(lengthBytes->bytes);
        delete lengthBytes;
        return 0;
    }
    int arrayLength = lengthBytes->length;
    jbyte *jbarray = lengthBytes->bytes;
    vec->insert(vec->end(), jbarray, jbarray + arrayLength);
    vec->insert(vec->end(), key, key + strlen(key));

    free(lengthBytes->bytes);
    delete lengthBytes;
    return 1;
}

int writeValue(JNIEnv *env, vector <jbyte> *vec, Value *value) {

    Length *lengthBytes = makeSingleLength(value->length);
    if (lengthBytes->length < 0) {
        free(lengthBytes->bytes);
        delete lengthBytes;
        return 0;
    }

    int arrayLength = lengthBytes->length;
    jbyte *jbarray = lengthBytes->bytes;
    vec->insert(vec->end(), jbarray, jbarray + arrayLength);
    vec->insert(vec->end(), value->val, value->val + value->length);

    free(jbarray);
    delete lengthBytes;
    if (value->expire > 0) {
        jbyte *b_count = longToByte(value->expire);
        vec->push_back(1);
        vec->insert(vec->end(), b_count, b_count + 8);
        free(b_count);
    } else {
        vec->push_back(0);
    }

    return 1;
}

void writeKeyValues(JNIEnv *env, FILE *fileHandle, hash_map<string, Value *> *cache) {
    int count = cache->size();

    jbyte *b_count = intToByte(count);
    fwrite(b_count, sizeof(jbyte), 4, fileHandle);
    free(b_count);
    int writeCount = 0;
    vector <jbyte> *vec = new vector<jbyte>();
    hash_map<string, Value *>::iterator iter;
    for (iter = cache->begin(); iter != cache->end(); iter++) {
        Value *value = iter->second;
        const char *str = iter->first.c_str();

        writeType(vec, value->type);
        if (writeKey(env, vec, str) && writeValue(env, vec, value)) {
            writeCount++;
            if (!vec->empty()) {
                jbyte *buffer = (jbyte *) malloc(vec->size());
                memcpy(buffer, vec->begin(), vec->size());
                fwrite(buffer, sizeof(jbyte), vec->size(), fileHandle);
                free(buffer);
            }
        } else {
            LOGE("WRITE RDB KV ERR %s", str);
        }
        vec->clear();
    }
    delete vec;
    if (writeCount != count) {
        long cur_pos = ftell(fileHandle);
        int rs = fseek(fileHandle, strlen(HEADER) + 4, SEEK_SET);
        if (rs == 0) {
            jbyte *b_count = intToByte(writeCount);
            fwrite(b_count, sizeof(jbyte), 4, fileHandle);
            free(b_count);
            LOGE("native modify count from %d to %d", count, writeCount);
            fseek(fileHandle, cur_pos, 0);
        }
    }

}

void syncRdb(JNIEnv *env, const char *cacheName, hash_map<string, Value *> *cache) {

    string bakFile(cacheName);
    string realFile(cacheName);
    bakFile.append(RDB_BAK);
    realFile.append(RDB_POSTFIX);

    FILE *fileHandle = fopen(bakFile.c_str(), "w");

    if (!fileHandle) {
        LOGE("error open %s", bakFile.c_str());
        return;
    }

    writeHead(fileHandle);

    writeKeyValues(env, fileHandle, cache);

    writeEof(fileHandle);

    fclose(fileHandle);

    int rs = rename(bakFile.c_str(), realFile.c_str());
}

void appendAof(JNIEnv *env, const char *cacheName, const char *key, Value *value, jbyte aofType) {
    char prefix[strlen(cacheName) + 1];
    prefix[strlen(cacheName)] = '\0';
    strcpy(prefix, cacheName);;

    char *fileName = strcat(prefix, AOF_POSTFIX);
    FILE *fileHandle = fopen(fileName, "a");

    if (!fileHandle) {
        LOGE("appendAof error open %s", fileName);
        return;
    }

    vector <jbyte> *vec = new vector<jbyte>();
    vec->push_back(aofType);

    if (writeKey(env, vec, key) && writeType(vec, value->type) && writeValue(env, vec, value)) {
        if (!vec->empty()) {
            jbyte *buffer = new jbyte[vec->size()];
            memcpy(buffer, vec->begin(), vec->size());
            fwrite(buffer, sizeof(jbyte), vec->size(), fileHandle);
            free(buffer);
        }
    } else {
        LOGE("ERR IN WRITE AOF %s with key: %s,type %d,len %d", cacheName, key, value->type,
             value->length);
    }
    vec->clear();
    delete vec;
    fclose(fileHandle);
}

void deleteAof(JNIEnv *env, const char *cacheName, const char *key) {
    char prefix[strlen(cacheName) + 1];
    prefix[strlen(cacheName)] = '\0';
    strcpy(prefix, cacheName);;

    char *fileName = strcat(prefix, AOF_POSTFIX);
    FILE *fileHandle = fopen(fileName, "a+");
    if (!fileHandle) {
        LOGE("error open %s", fileName);
        return;
    }

    vector <jbyte> *vec = new vector<jbyte>();
    vec->push_back(2);
    if (writeKey(env, vec, key)) {
        if (!vec->empty()) {
            jbyte *buffer = new jbyte[vec->size()];
            memcpy(buffer, vec->begin(), vec->size());
            fwrite(buffer, sizeof(jbyte), vec->size(), fileHandle);
            free(buffer);
        }
    } else {
        LOGE("ERR IN DELETE AOF %s with key: %s", cacheName, key);
    }
    vec->clear();
    delete vec;
    fclose(fileHandle);
}

void syncAof(JNIEnv *env, const char *cacheName, hash_map<string, Value *> *temp) {

    string bakFile(cacheName);
    string realFile(cacheName);
    bakFile.append(AOF_BAK);
    realFile.append(AOF_POSTFIX);


    FILE *fileHandle = fopen(bakFile.c_str(), "w");
    if (!fileHandle) {
        LOGE("error open %s", bakFile.c_str());
        return;
    }

    vector <jbyte> *vec = new vector<jbyte>();
    jbyte putType = 1;

    hash_map<string, Value *>::iterator iter;
    for (iter = temp->begin(); iter != temp->end(); iter++) {
        Value *value = iter->second;
        const char *str = iter->first.c_str();
        vec->push_back(1);
        if (writeKey(env, vec, str) && writeType(vec, value->type) &&
            writeValue(env, vec, value)) {
            if (!vec->empty()) {
                jbyte *buffer = new jbyte[vec->size()];
                memcpy(buffer, vec->begin(), vec->size());
                fwrite(buffer, sizeof(jbyte), vec->size(), fileHandle);
                free(buffer);
            }
        } else {
            LOGE("ERR IN WRITE AOF %s with key: %s,type %d,len %d", cacheName, str, value->type,
                 value->length);
        }
        vec->clear();
    }
    delete vec;
    fclose(fileHandle);

    rename(bakFile.c_str(), realFile.c_str());
}

/**
 * READ
 */

lru *readRdb(JNIEnv *env, const char *cacheName) {
    char prefix[strlen(cacheName) + 1];
    prefix[strlen(cacheName)] = '\0';
    strcpy(prefix, cacheName);

    lru *l = new lru();
    char *fileName = strcat(prefix, RDB_POSTFIX);

    if (access(fileName, 0)) {
        int fd = open(fileName, O_CREAT, S_IRUSR | S_IWUSR);
        int result = flock(fd, LOCK_EX | LOCK_NB);
        if (result < 0) {
            return NULL;
        }
        LOGE("%s not exist", fileName);
        return l;
    }
    FILE *fileHandle = fopen(fileName, "rw");

    if (!fileHandle) {
        LOGE("error open %s", fileName);
        return NULL;
    }

    int fd = open(fileName, O_RDWR);
    int result = flock(fd, LOCK_EX | LOCK_NB);
    LOGE("FD:%d flock:%d", fd, result);
    if (result < 0) {
        return NULL;
    }

    char headStr[7] = {'\0'};
    char eofStr[11] = {'\0'};

    fread(headStr, sizeof(char), strlen(HEADER), fileHandle);

    LOGD("read head:%s", headStr);

    if (strcmp(headStr, HEADER) == 0) {
        jbyte versionBytes[4] = {0};
        fread(versionBytes, sizeof(jbyte), 4, fileHandle);
        int version = byteToInt(versionBytes);
        //LOGD("read version:%d", version);

        jbyte numBytes[4] = {0};
        fread(numBytes, sizeof(jbyte), 4, fileHandle);
        int number = byteToInt(numBytes);
        //LOGD("read num:%d", number);

        if (number < MAX_KV_PAIRS) {
            jbyte hasExpire = 0;
            for (int i = 0; i < number; ++i) {
                jbyte type = 0;
                fread(&type, sizeof(jbyte), 1, fileHandle);

                int keyLength = getSingleLength(fileHandle);

                if (keyLength > 0 && keyLength <= MAX_KEY_VALUE_LENGTH) {   //合法数据
                    char *key = (char *) malloc(keyLength + 1);
                    key[keyLength] = '\0';
                    int readLen = fread(key, sizeof(char), keyLength, fileHandle);
                    if (readLen < keyLength) {
                        free(key);
                        LOGE("ERR IN READ RDB %s, want key length: %d read: %d", cacheName,
                             keyLength, readLen);
                        break;
                    }

                    int valueLength = getSingleLength(fileHandle);
                    if (valueLength > 0 && valueLength <= MAX_KEY_VALUE_LENGTH) { //合法数据
                        jbyte *values = (jbyte *) malloc(valueLength);
                        readLen = fread(values, sizeof(jbyte), valueLength, fileHandle);
                        if (readLen < valueLength) {
                            LOGE("ERR IN READ RDB %s, want value length: %d read: %d with key %s",
                                 cacheName, valueLength, readLen, key);
                            free(key);
                            free(values);
                            break;
                        }

                        fread(&hasExpire, sizeof(jbyte), 1, fileHandle);
                        long expire = 0;
                        if (hasExpire > 0) {
                            jbyte expBytes[8];
                            fread(expBytes, sizeof(jbyte), 8, fileHandle);
                            expire = byteToLong(expBytes);
                        }
                        Value *val = new Value();
                        val->type = type;
                        val->length = valueLength;
                        val->val = values;
                        val->expire = expire;

                        l->put(string(key), val);
                        free(key);
                    } else {
                        LOGE("ERR IN READ RDB %s OVER VALUELENGTH: %d with key: %s", cacheName,
                             valueLength, key);
                        free(key);
                        break;
                    }
                } else {
                    LOGE("ERR IN READ RDB %s OVER KEYLENGTH: %d", cacheName, keyLength);
                    break;
                }
            }
            fread(eofStr, sizeof(char), strlen(EOF_STR), fileHandle);
            LOGD("eof:%s", eofStr);
        } else {
            LOGE("OVER MAX KV PAIRS IN READ RDB %s", cacheName);
        }
    } else {
        LOGE("%s not a native rdb file", fileName);
    }

    fclose(fileHandle);
    return l;
}

lru *readAof(JNIEnv *env, const char *cacheName) {

    char prefix[strlen(cacheName) + 1];
    prefix[strlen(cacheName)] = '\0';
    strcpy(prefix, cacheName);
    lru *l = new lru();
    char *fileName = strcat(prefix, AOF_POSTFIX);

    if (access(fileName, F_OK)) {
        int fd = open(fileName, O_CREAT, S_IRUSR | S_IWUSR);
        int result = flock(fd, LOCK_EX | LOCK_NB);
        if (result < 0) {
            return NULL;
        }
        LOGE("%s not exist", fileName);
        return l;
    }

    FILE *fileHandle = fopen(fileName, "rw");

    if (!fileHandle) {
        LOGE("error open %s", fileName);
        return NULL;
    }

    int fd = open(fileName, O_RDWR);
    int result = flock(fd, LOCK_EX | LOCK_NB);
    LOGE("FD:%d flock:%d", fd, result);
    if (result < 0) {
        return NULL;
    }

    jbyte aofType[1] = {0};
    jbyte hasExpire = 0;
    while (fread(aofType, sizeof(jbyte), 1, fileHandle) > 0) {
        if (aofType[0] == AOF_SET) {
            int keyLength = getSingleLength(fileHandle);

            if (keyLength > 0 && keyLength <= MAX_KEY_VALUE_LENGTH) {

                char *key = (char *) malloc(keyLength + 1);
                key[keyLength] = '\0';

                int readLen = fread(key, sizeof(char), keyLength, fileHandle);

                jbyte valueType = 0;
                fread(&valueType, sizeof(jbyte), 1, fileHandle);

                int valueLength = getSingleLength(fileHandle);
                if (valueLength > 0 && valueLength <= MAX_KEY_VALUE_LENGTH) {
                    jbyte *values = (jbyte *) malloc(valueLength);
                    fread(values, sizeof(jbyte), valueLength, fileHandle);

                    fread(&hasExpire, sizeof(jbyte), 1, fileHandle);
                    long expire = 0;
                    if (hasExpire > 0) {
                        jbyte expBytes[8];
                        fread(expBytes, sizeof(jbyte), 8, fileHandle);
                        expire = byteToLong(expBytes);
                    }
                    Value *val = new Value();
                    val->type = valueType;
                    val->length = valueLength;
                    val->val = values;
                    val->expire = expire;

                    l->put(string(key), val);
//                    free(values);
//                    delete val;
                    free(key);
                } else {
                    LOGE("ERR IN READ AOF %s OVER VALUELENGTH: %d with key: %s", cacheName,
                         valueLength, key);
                    free(key);
                    break;
                }
            } else {
                LOGE("ERR IN READ AOF %s OVER KEYLENGTH: %d", cacheName, keyLength);
            }
        } else if (aofType[0] == AOF_DELETE) {
            int keyLength = getSingleLength(fileHandle);
            if (keyLength > 0 && keyLength <= MAX_KEY_VALUE_LENGTH) {
                char *key = (char *) malloc(keyLength + 1);
                key[keyLength] = '\0';
                fread(key, sizeof(char), keyLength, fileHandle);

                l->remove(string(key));
                free(key);
            } else {
                LOGE("ERR IN READ AOF %s OVER KEYLENGTH: %d", cacheName, keyLength);
            }
        } else if (aofType[0] == AOF_LADD) {
            int keyLength = getSingleLength(fileHandle);
            if (keyLength > 0 && keyLength <= MAX_KEY_VALUE_LENGTH) {
                char *key = (char *) malloc(keyLength + 1);
                key[keyLength] = '\0';
                fread(key, sizeof(char), keyLength, fileHandle);

                jbyte valueType = 0;
                fread(&valueType, sizeof(jbyte), 1, fileHandle);

                int valueLength = getSingleLength(fileHandle);
                if (valueLength > 0 && valueLength <= MAX_KEY_VALUE_LENGTH) {
                    jbyte *values = (jbyte *) malloc(valueLength);
                    fread(values, sizeof(jbyte), valueLength, fileHandle);

                    fread(&hasExpire, sizeof(jbyte), 1, fileHandle);
                    long expire = 0;
                    if (hasExpire > 0) {
                        jbyte expBytes[8];
                        fread(expBytes, sizeof(jbyte), 8, fileHandle);
                        expire = byteToLong(expBytes);
                    }

                    l->ladd(string(key), valueType, values, valueLength);

                    free(key);
                } else {
                    LOGE("ERR IN READ AOF %s OVER VALUELENGTH: %d with key: %s", cacheName,
                         valueLength, key);
                    free(key);
                    break;
                }

            } else {
                LOGE("ERR IN READ AOF %s OVER KEYLENGTH: %d", cacheName, keyLength);
            }

        } else {
            LOGE("error aof type %d %s", aofType[0], fileName);
            break;
        }
    }
    fclose(fileHandle);

    return l;
}