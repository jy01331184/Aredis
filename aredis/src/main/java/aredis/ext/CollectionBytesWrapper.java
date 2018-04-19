package aredis.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import aredis.AType;

/**
 * Created by tianyang on 17/12/13.
 */
public class CollectionBytesWrapper implements BytesWrapper {

    private Collection collection;

    private boolean init;

    private List<byte[]> resultBytes = new ArrayList<>();
    private byte[] totalLength;

    public CollectionBytesWrapper(Collection col) {
        this.collection = col;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public byte[] get(int index) throws ARedisException {
        if (!init) {
            init();
        }

        return resultBytes.get(index);
    }

    @Override
    public byte[] getHeader() throws ARedisException {
        if (!init) {
            init();
        }

        return totalLength;
    }

    private void init() throws ARedisException {
        int total = 0;

        for (Object object : collection) {
            byte type = AType.getType(object);

            BytesWrapper wrapper = AType.getTypeBytesWrapper(object);
            int childLen = 0;
            byte[] header = wrapper.getHeader();
            for (int j = 0; j < wrapper.size(); j++) {
                childLen += wrapper.get(j).length;
            }
            byte[] rs = new byte[header.length + 1 + childLen];
            rs[0] = type;
            System.arraycopy(header, 0, rs, 1, header.length);
            int startIndex = 1 + header.length;
            for (int j = 0; j < wrapper.size(); j++) {
                byte[] temp = wrapper.get(j);
                System.arraycopy(temp, 0, rs, startIndex, temp.length);
                startIndex += temp.length;
            }

            resultBytes.add(rs);

            total += rs.length;
        }

        totalLength = Util.makeSingleLength(total);
        init = true;
    }
}
