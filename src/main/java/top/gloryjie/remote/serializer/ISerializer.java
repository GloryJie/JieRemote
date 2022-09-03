package top.gloryjie.remote.serializer;

import java.util.HashMap;

/**
 * 序列号策略接口
 *
 * @author jie-r
 * @since 2022/8/8
 */
public interface ISerializer {

    /**
     * 序列化
     * * @return
     */
    byte[] writeHeader(HashMap<String, String> headerMap) throws Exception;


    /**
     * 反序列化
     *
     * @param data
     * @return
     */
    HashMap<String, String> readHeader(byte[] data) throws Exception;

    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    byte[] writeBody(Object obj) throws Exception;


    /**
     * 反序列化
     *
     * @param data
     * @param cls
     * @return
     */
    Object readBody(byte[] data, Class<?> cls) throws Exception;


}
