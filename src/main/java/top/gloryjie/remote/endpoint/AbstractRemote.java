package top.gloryjie.remote.endpoint;

import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.connection.Connection;
import top.gloryjie.remote.msg.InnerMsgType;
import top.gloryjie.remote.msg.MsgExecutorSelector;
import top.gloryjie.remote.msg.RemoteMsg;
import top.gloryjie.remote.msg.RemoteMsgHandler;
import top.gloryjie.remote.serializer.Hessian2Serializer;
import top.gloryjie.remote.serializer.ISerializer;
import top.gloryjie.remote.serializer.InnerSerializer;
import top.gloryjie.remote.serializer.JdkSerializer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * client&server common logic
 *
 * @author jie-r
 * @since 2022/8/18
 */
@Slf4j
public class AbstractRemote {

    protected final Map<Integer, CompletableFuture<RemoteMsg<?>>> responseFutureMap = new ConcurrentHashMap<>();

    protected Map<Integer, Class<?>> msgTypeMap = new ConcurrentHashMap<>();
    protected Map<Integer, RemoteMsgHandler> msgHandlerMap = new ConcurrentHashMap<>();
    protected Map<Integer, ISerializer> serializerMap = new ConcurrentHashMap<>();
    protected MsgExecutorSelector executorSelector;


    public AbstractRemote(MsgExecutorSelector executorSelector) {
        this.executorSelector = executorSelector;
    }

    protected void serializeRemoteMsg(RemoteMsg<?> msg) throws Exception {
        ISerializer serializer = serializerMap.get(msg.getSerializeType());
        if (msg.getHeaderExt() != null && !msg.getHeaderExt().isEmpty()) {
            msg.setHeaderExtBytes(serializer.writeHeader(msg.getHeaderExt()));
        }
        if (msg.getBody() != null) {
            msg.setBodyBytes(serializer.writeBody(msg.getBody()));
        }
    }


    protected void sendOnewayImpl(Connection connection, RemoteMsg<?> msg, long timeoutMillis) {


    }

    protected void registerInnerSerializer() {
        serializerMap.put(InnerSerializer.JDK.getCode(), new JdkSerializer());
        serializerMap.put(InnerSerializer.HESSIAN2.getCode(), new Hessian2Serializer());
    }

    protected void registerDefaultMsgType() {
        msgTypeMap.put(InnerMsgType.REMOTE_EXCEPTION.getMsgType(), InnerMsgType.REMOTE_EXCEPTION.getClazz());
        msgTypeMap.put(InnerMsgType.SERVER_SHUTDOWN.getMsgType(), InnerMsgType.SERVER_SHUTDOWN.getClazz());
    }



}
