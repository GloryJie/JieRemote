package top.gloryjie.remote.protocol.msg;

import lombok.Getter;
import lombok.Setter;
import top.gloryjie.remote.exception.ExceptionProtocol;
import top.gloryjie.remote.serializer.ISerializer;
import top.gloryjie.remote.serializer.InnerSerializer;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在JieProtocol基础上的事件模型
 *
 * @author jie-r
 * @since 2022/8/8
 */
@Setter
@Getter
public class RemoteMsg<T> {

    // 魔数，数据包的起点，1byte 0xd8，JIE字母的asii码之和
    public static final int JIE_MAGIC_CODE = 0xd8;
    public static final int JIE_MAGIC_CODE_BYTES = 2;
    public static final int DESC_TOTAL_LEN_BYTES = 4;
    public static final int DESC_HEADER_LEN_BYTES = 2;
    // HEADER_FIX_BYTES not include
    public static final int HEADER_FIX_BYTES = 9;

    public static final int REQ_TYPE = 0;
    public static final int RESP_TYPE = 1;
    public static final int ONE_WAY_TYPE = 2;

    public static final int COMPRESS_TYPE_NONE = 0;

    private static AtomicInteger msg_ID = new AtomicInteger(0);

    /**
     * 序列化方式,1byte
     */
    private int serializeType;

    /**
     * 压缩方式,1byte
     */
    private int compressType;

    /**
     * 消息类型, 2byte
     */
    private int msgType;

    /**
     * 消息id，4byte
     */
    private int msgId = msg_ID.incrementAndGet();


    /**
     * 标记位，1个字节
     */
    private int flag;

    /**
     * 头部拓展数据, 直接使用map结构
     */
    private HashMap<String, String> headerExt;

    private byte[] headerExtBytes;

    /**
     * body数据部分
     */
    private volatile T body;
    private byte[] bodyBytes;

    public RemoteMsg() {
    }


    public boolean isRequest() {
        int bits = 1 << REQ_TYPE;
        return (this.flag & bits) == bits;
    }

    public boolean isResponse() {
        int bits = 1 << RESP_TYPE;
        return (this.flag & bits) == bits;    }

    public boolean isOneway() {
        int bits = 1 << ONE_WAY_TYPE;
        return (this.flag & bits) == bits;
    }

    public void markReqFlag() {
        int bits = 1 << REQ_TYPE;
        this.flag |= bits;
    }

    public void markRespFlag() {
        int bits = 1 << RESP_TYPE;
        this.flag |= bits;
    }

    public void markOnewayFlag() {
        int bits = 1 << ONE_WAY_TYPE;
        this.flag |= bits;
    }


    public static RemoteMsg<ExceptionProtocol> createExceptionResponse(RemoteMsg<?> reqMsg, ExceptionProtocol exceptionProtocol){
        RemoteMsg<ExceptionProtocol> respMsg = new RemoteMsg<>();
        respMsg.setSerializeType(InnerSerializer.HESSIAN2.getCode());
        respMsg.setCompressType(reqMsg.getCompressType());
        respMsg.setMsgId(reqMsg.getMsgId());
        respMsg.setBody(exceptionProtocol);

        respMsg.setMsgType(InnerMsgType.REMOTE_EXCEPTION.getMsgType());
        respMsg.markRespFlag();
        return respMsg;
    }

    public static RemoteMsg<?> createResponse(RemoteMsg<?> reqMsg){
        RemoteMsg<ExceptionProtocol> respMsg = new RemoteMsg<>();
        respMsg.setSerializeType(reqMsg.getSerializeType());
        respMsg.setCompressType(reqMsg.getCompressType());
        respMsg.setMsgId(reqMsg.getMsgId());
        respMsg.setMsgType(reqMsg.getMsgType());
        respMsg.markRespFlag();
        return respMsg;
    }

    public void deserialize(ISerializer serializer, Class<?> clzz) throws Exception {
        if (headerExtBytes != null) {
            this.headerExt = serializer.readHeader(headerExtBytes);
        }
        if (bodyBytes != null && bodyBytes.length > 0) {
            this.body = (T) serializer.readBody(bodyBytes, clzz);
        }
    }





}
