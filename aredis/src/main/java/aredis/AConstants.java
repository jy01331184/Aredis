package aredis;

/**
 * Created by tianyang on 17/10/16.
 */
public class AConstants {

    public static final int AREDIS_VERSION = 1;

    public static final String HEADER_STR = "AREDIS";

    public static final byte[] HEADER = HEADER_STR.getBytes();

    public static final int HEADER_LENGTH = HEADER.length;

    public static final String EOF_STR = "AREDIS_EOF";

    public static final byte[] EOF = EOF_STR.getBytes();

    public static final int EOF_LENGTH = EOF.length;
}
