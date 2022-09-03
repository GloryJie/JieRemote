package top.gloryjie.remote.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * @author jie-r
 * @since 2022/8/13
 */
public class JdkSerializer implements ISerializer {

    @Override
    public byte[] writeHeader(HashMap<String, String> headerMap) throws Exception {
        return this.writeBody(headerMap);
    }

    @Override
    public HashMap<String, String> readHeader(byte[] data) throws Exception {
        return (HashMap<String, String>) readBody(data, HashMap.class);
    }

    @Override
    public byte[] writeBody(Object obj) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        }
    }

    @Override
    public Object readBody(byte[] data, Class<?> cls) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        }
    }
}
