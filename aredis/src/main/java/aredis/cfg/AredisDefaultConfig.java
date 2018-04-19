package aredis.cfg;

/**
 * Created by tianyang on 18/1/30.
 */
public class AredisDefaultConfig implements AredisConfig {

    @Override
    public AredisType getType() {
        return AredisType.RDB;
    }

    @Override
    public int getRdbSaveCount() {
        return 50;
    }

    @Override
    public long getRdbSaveInterval() {
        return 10*60*1000;
    }

    @Override
    public boolean useAofStrictMode() {
        return false;
    }

    @Override
    public int getMaxIoWaitTime() {
        return 1000;
    }

    @Override
    public boolean persist() {
        return true;
    }
}
