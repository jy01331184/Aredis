#include "const.h"


pthread_mutex_t mutex_t = PTHREAD_MUTEX_INITIALIZER;
int MAX_KV_PAIRS = 90000;
int MAX_KEY_VALUE_LENGTH = 4194304;
int MAX_MEM_SIZE = 20 * 1024 * 1024;

int AOF_SET = 1;
int AOF_DELETE = 2;
int AOF_LADD = 3;