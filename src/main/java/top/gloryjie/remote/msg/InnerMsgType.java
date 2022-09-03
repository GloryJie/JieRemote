package top.gloryjie.remote.msg;

import lombok.Getter;
import top.gloryjie.remote.exception.ExceptionProtocol;

/**
 * @author jie-r
 * @since 2022/9/2
 */
@Getter
public enum InnerMsgType {

    REMOTE_EXCEPTION(1, ExceptionProtocol.class),
    SERVER_SHUTDOWN(2, String.class);


    private int msgType;
    private Class<?> clazz;


    InnerMsgType(int msgType, Class<?> clazz) {
        this.msgType = msgType;
        this.clazz = clazz;
    }

    public static boolean isInnerMsgType(int msgType){
        return msgType == 1 || msgType == 2;
    }

    public static InnerMsgType getByCode(int msgType){
        for (InnerMsgType value : InnerMsgType.values()) {
            if (value.getMsgType() == msgType){
                return value;
            }
        }
        return null;
    }
}
