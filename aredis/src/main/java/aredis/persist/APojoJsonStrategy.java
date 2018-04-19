package aredis.persist;

import com.alibaba.fastjson.JSON;

/**
 * Created by tianyang on 17/12/28.
 */
public class APojoJsonStrategy implements APojoStrategy {


    @Override
    public byte[] toBytes(Object object) {

        String json = JSON.toJSONString(object);

        return json.getBytes();
    }

    @Override
    public Object toObject(byte[] bytes, Class cls) {

        Object obj = JSON.parseObject(bytes, cls, null);
        return obj;
    }
}
