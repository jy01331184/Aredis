package aredis.ext;

/**
 * Created by tianyang on 17/12/29.
 */
public class ARedisException extends Exception {


    public ARedisException(String message) {
        super(message);
    }

    public ARedisException(String message, Throwable cause) {
        super(message, cause);
    }
}
