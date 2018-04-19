package aredis.cfg;

/**
 * Created by tianyang on 18/1/30.
 */
public interface AredisConfig {

    /**
     * return 使用rdb 或aof 模式,默认是rdb模式
     */
    AredisType getType();

    /**
     *  return rdb模式下每隔 多少次set/delete 操作后 写一次镜像 默认为50次
     *  如果返回<=0 则不会自动触发持久化操作
     */
    int getRdbSaveCount();

    /**
     * return rdb模式下每隔 多少毫秒 写一次镜像 默认为10分钟
     *  如果返回<=0 则不会自动触发持久化操作
     */
    long getRdbSaveInterval();

    /**
     * @return aof模式下 是否使用异步io ,默认false
     */
    boolean useAofStrictMode();

    /**
     * @return 异步io的最大等待超时的毫秒数时间 默认为1000ms
     */
    int getMaxIoWaitTime();

    /**
     * @return 是否持久化数据  默认true
     */
    boolean persist();

    static enum AredisType{
        AOF,RDB;
    }
}
