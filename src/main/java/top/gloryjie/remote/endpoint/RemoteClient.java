package top.gloryjie.remote.endpoint;

import top.gloryjie.remote.msg.RemoteMsg;
import top.gloryjie.remote.msg.RemoteMsgHandler;
import top.gloryjie.remote.serializer.ISerializer;

import java.util.concurrent.CompletableFuture;

/**
 * @author jie-r
 * @since 2022/8/20
 */
public interface RemoteClient extends RemoteService{

    void registerMsgTypeAndHandler(int msgType, Class<?> bodyType, RemoteMsgHandler handler);

    void registerSerializer(int type, ISerializer serializer);

    /**
     * @param addr          ip:port
     * @param msg
     * @param timeoutMillis
     * @return
     */
    RemoteMsg<?> send(String addr, RemoteMsg<?> msg, long timeoutMillis);

    CompletableFuture<RemoteMsg<?>> sendAsync(String addr, RemoteMsg<?> msg, long timeoutMillis);

    void sendOneway(String addr, RemoteMsg<?> msg, long timeoutMillis);

}
