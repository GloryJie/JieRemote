package top.gloryjie.remote.endpoint;

import top.gloryjie.remote.connection.Connection;
import top.gloryjie.remote.protocol.msg.RemoteMsg;
import top.gloryjie.remote.protocol.msg.RemoteMsgHandler;
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
     * @param connection
     * @param msg
     * @param timeoutMillis
     * @return
     */
    RemoteMsg<?> send(Connection connection, RemoteMsg<?> msg, long timeoutMillis);

    CompletableFuture<RemoteMsg<?>> sendAsync(Connection connection, RemoteMsg<?> msg, long timeoutMillis);

    void sendOneway(Connection connection, RemoteMsg<?> msg, long timeoutMillis);

    Connection connect(String addr, long timeoutMills);

    void closeConnection(Connection connection);

}
