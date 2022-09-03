package top.gloryjie.remote.endpoint;

import top.gloryjie.remote.msg.MsgExecutorSelector;
import top.gloryjie.remote.msg.RemoteMsgHandler;
import top.gloryjie.remote.serializer.ISerializer;

/**
 * @author jie-r
 * @since 2022/8/18
 */
public interface RemoteServer extends RemoteService {

    void registerMsgTypeAndHandler(int msgType, Class<?> bodyType, RemoteMsgHandler handler);

    void registerExecutorSelector(MsgExecutorSelector selector);

    void registerSerializer(int type, ISerializer serializer);

}
