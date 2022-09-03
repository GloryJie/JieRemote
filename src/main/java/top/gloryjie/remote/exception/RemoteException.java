package top.gloryjie.remote.exception;

/**
 * @author jie-r
 * @since 2022/8/13
 */
public class RemoteException extends RuntimeException {

    /**
     * 协议层错误码，1~100
     */
    public static final int MAGIC_CODE_NOT_VALID = 1;


    /**
     *
     */
    public static final int OTHER_ERR = 500;
    public static final int COMMON_TIME_OUT = 501;


    /**
     * client err
     */
    public static final int CLIENT_ENCODE_ERR = 101;
    public static final int CLIENT_DECODE_ERR = 102;
    public static final int CLIENT_SERIALIZE_ERR = 103;
    public static final int CLIENT_DESERIALIZE_ERR = 104;
    public static final int CLIENT_CONNECT_ERR = 105;
    public static final int CLIENT_WRITE_DATA_ERR = 106;
    public static final int CLIENT_REQ_TIMEOUT = 107;
    public static final int CLIENT_CONNECTION_NOT_ACTIVE = 108;


    /**
     * server err
     */
    public static final int SERVER_ENCODE_ERR = 201;
    public static final int SERVER_DECODE_ERR = 202;
    public static final int SERVER_SERIALIZE_ERR = 203;
    public static final int SERVER_DESERIALIZE_ERR = 204;
    public static final int SERVER_NOT_SUPPORT_MSG_TYPE_ERR = 205;
    public static final int SERVER_HANDLER_ERR = 206;
    public static final int SERVER_EXECUTOR_ABORT = 206;


    private final int code;

    public RemoteException(int code, String message) {
        super(code + " , " + message);
        this.code = code;
    }

    public RemoteException(int code, String message, Throwable cause) {
        super(code + " : " + message, cause);
        this.code = code;
    }

    public RemoteException(int code, Throwable cause) {
        super(code + ", " + cause.getMessage(), cause);
        this.code = code;
    }

}
