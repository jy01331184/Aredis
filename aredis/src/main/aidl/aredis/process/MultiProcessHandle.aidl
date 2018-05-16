// MultiProcessHandle.aidl
package aredis.process;
import aredis.NativeRecord;
// Declare any non-default types here with import statements

interface MultiProcessHandle {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    NativeRecord get(String node,String key);

    void set(String node,String key,byte type,in byte[] data,long expire);
}
