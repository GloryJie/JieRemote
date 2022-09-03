package top.gloryjie.remote.serializer;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 * @author jie-r
 * @since 2022/9/3
 */
public class Hessian2Serializer implements ISerializer {

    @Override
    public byte[] writeHeader(HashMap<String, String> headerMap) throws Exception {
        return this.writeBody(headerMap);
    }

    @Override
    public HashMap<String, String> readHeader(byte[] data) throws Exception {
        return (HashMap<String, String>) this.readBody(data, HashMap.class);
    }

    @Override
    public byte[] writeBody(Object obj) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Hessian2Output hessian2Output = new Hessian2Output(outputStream);
            hessian2Output.writeObject(obj);
            hessian2Output.flush();
            return outputStream.toByteArray();
        }
    }

    @Override
    public Object readBody(byte[] data, Class<?> cls) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            Hessian2Input hessian2Input = new Hessian2Input(inputStream);
            return hessian2Input.readObject(cls);
        }
    }

}
