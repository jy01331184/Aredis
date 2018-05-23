//
// Created by tianyang on 18/1/3.
//
#pragma once

#include "const.h"

using namespace std;
const int STATE_FREE = 0, STATE_FORKING = 1, STATE_FORKED = 2;

class lru {
public:

    lru() {
        head = new Value();
        tail = new Value();
        cache = new hash_map<string, Value *>();
        tempPut = new hash_map<string, Value *>();
        head->next = tail;
        tail->prev = head;
        tempDel = new list<string>();

    }

    ~lru() {
        LOGD("dispose lru");
        hash_map<string, Value *>::iterator iter;
        for (iter = begin(); iter != end(); iter++) {
            Value *val = iter->second;
            free(val->val);
            delete val;
        }

        cache->clear();
        delete cache;
        delete head;
        delete tail;
        delete tempPut;
        delete tempDel;
    }

    void startFork() {
        state = STATE_FORKING;
    }

    void endFork() {
        state = STATE_FORKED;
    }

    void put(string key, Value *value) {

        if (tempSet(key, value) == 1) {
            return;
        }
        simplePut(key, value, true);
    }

    Value *get(string key) {

        if (hasTempDelete(key) > 0) {
            return NULL;
        }

        Value *value = tempGet(key);
        if (value) {
            return value;
        }

        if (cache->count(key) > 0) {
            Value *value = cache->operator[](key);
            time_t systime;
            systime = time(NULL) * 1000;
            long dif = systime - value->expire;

            if (value->expire == 0 || dif <= 0) {
                dettach(value);
                attach(value);
                return value;
            } else {
                LOGD("expire key:%s", key.c_str());
                cache->erase(key);
                memSize -= value->length;
                dettach(value);
                free(value->val);
                delete value;
            }
        }

        return NULL;
    }

    void remove(string key) {
        if (tempDelete(key) == 1) {
            return;
        }
        simpleDelete(key);
    }

    bool ladd(string key, jbyte type, jbyte *value, int length) {

        Value *val = get(key);
        if (val) {
            if (val->type == -10) {
                Length *len = makeSingleLength(length);

                val->val = (jbyte *) realloc(val->val, length + val->length + 1 + len->length);

                memcpy(val->val + val->length, &type, sizeof(jbyte)); //cp type
                memcpy(val->val + val->length + 1, len->bytes, len->length);//cp len
                memcpy(val->val + +val->length + 1 + len->length, value, length);//cp data
                val->length = val->length + length + 1 + len->length;
                memSize += length + 1 + len->length;
                free(value);
                delete len;
            } else {
                LOGD("ladd value not a list with key %s", key.c_str());
                free(value);
                return 0;
            }
        } else {
            Length *len = makeSingleLength(length);
            jbyte *jbarray = (jbyte *) malloc(length + 1 + len->length);
            memcpy(jbarray, &type, sizeof(jbyte));
            memcpy(jbarray + 1, len->bytes, len->length);
            memcpy(jbarray + 1 + len->length, value, length);

            Value *val = new Value();

            val->length = length + 1 + len->length;
            val->type = -10;
            val->val = jbarray;

            put(key, val);
            delete len;
        }

        return 1;
    }

    hash_map<string, Value *>::iterator begin() {
        return cache->begin();
    }

    hash_map<string, Value *>::iterator end() {
        return cache->end();
    };

    int size() {
        return cache->size();
    }

    hash_map<string, Value *> *getCache() {
        //LOGD("get ca %d %p",state,cache);
        recover();
        return cache;
    };

private:
    int state = 0;
    Value *head, *tail;
    hash_map<string, Value *> *cache;
    hash_map<string, Value *> *tempPut;
    list <string> *tempDel;
    int memSize;

    void recover() {
        if (state == STATE_FORKED) {
            hash_map<string, Value *>::iterator iter;
            for (iter = tempPut->begin(); iter != tempPut->end(); iter++) {
                string tempKey = iter->first;
                Value *tempVal = iter->second;
                //LOGD("TEMP REC:%s", tempKey.c_str());
                simplePut(tempKey, tempVal, false);
            }
            tempPut->clear();

            list<string>::iterator liter;
            for (liter = tempDel->begin(); liter != tempDel->end(); liter++) {
                string tempKey = *liter;
                simpleDelete(tempKey);
                //LOGD("TEMP DEL REC:%s", tempKey.c_str());
            }
            tempDel->clear();
            state = STATE_FREE;
        }
    }

    int tempSet(string key, Value *value) {
        if (state == STATE_FORKING) {
            if (tempPut->count(key) > 0) {
                Value *oldValue = tempPut->operator[](key);
                tempPut->erase(key);
                free(oldValue->val);
                delete oldValue;
            }
            tempPut->operator[](key) = value;
            tempDel->remove(key);
            //LOGD("TEMP PUT:%s %p", key.c_str(),cache);
            return 1;
        }
        recover();
        return 0;
    }

    int tempDelete(string key) {
        if (state == STATE_FORKING) {
            //LOGD("TEMP DEL:%s", key.c_str());
            tempDel->push_back(key);
            tempPut->erase(key);
            return 1;
        }
        recover();
        return 0;
    }

    int hasTempDelete(string key) {
        if (state == STATE_FORKING) {

            int count = std::count(tempDel->begin(), tempDel->end(), key);

            return count;
        }
        return 0;
    }

    Value *tempGet(string key) {
        if (state == STATE_FORKING) {
            if (tempPut->count(key) > 0) {
                Value *value = tempPut->operator[](key);

                time_t systime;
                systime = time(NULL) * 1000;
                long dif = systime - value->expire;

                if (value->expire == 0 || dif <= 0) {
                    return value;
                } else {
                    LOGD("expire temp key:%s", key.c_str());
                    tempPut->erase(key);
                    free(value->val);
                    delete value;
                }
            }
        } else {
            recover();
        }

        return NULL;
    }


    void simplePut(string key, Value *value, bool shouldTrim) {

        if (cache->count(key) > 0) {
            Value *oldValue = cache->operator[](key);
            cache->erase(key);
            //LOGD("subsitute key:%s", key.c_str());
            memSize -= oldValue->length;
            dettach(oldValue);
            free(oldValue->val);
            delete oldValue;
        }

        value->key = key;
        attach(value);
        memSize += value->length;

        //cache->insert(pair<string, Value *>(key,value));
        cache->operator[](key) = value;

        while (shouldTrim && (memSize > MAX_MEM_SIZE && size() > 0) || size() > MAX_KV_PAIRS) {
            trim();
        }
    }

    void simpleDelete(string key) {
        if (cache->count(key) > 0) {
            Value *oldValue = cache->operator[](key);
            cache->erase(key);
            memSize -= oldValue->length;
            dettach(oldValue);
            free(oldValue->val);
            delete oldValue;
        }
    }

    void attach(Value *value) {
        Value *oldNext = head->next;
        oldNext->prev = value;
        head->next = value;
        value->next = oldNext;
        value->prev = head;
    }

    void dettach(Value *value) {
        value->prev->next = value->next;
        value->next->prev = value->prev;
    }

    void trim() {
        const char *key = tail->prev->key.c_str();
        LOGD("trim key %s. after count %d,memsize %d", key, size(), memSize);
        remove(tail->prev->key);


    }
};



