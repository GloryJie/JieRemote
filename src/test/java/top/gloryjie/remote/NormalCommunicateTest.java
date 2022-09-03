package top.gloryjie.remote;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.gloryjie.remote.client.ClientConfig;
import top.gloryjie.remote.client.NettyRemoteClient;
import top.gloryjie.remote.endpoint.RemoteClient;
import top.gloryjie.remote.msg.RemoteMsg;
import top.gloryjie.remote.msg.RemoteMsgContext;
import top.gloryjie.remote.msg.RemoteMsgHandler;
import top.gloryjie.remote.serializer.InnerSerializer;
import top.gloryjie.remote.server.NettyRemoteServer;
import top.gloryjie.remote.server.ServerConfig;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * @author jie-r
 * @since 2022/9/3
 */
@Slf4j
public class NormalCommunicateTest {

    public static final int CUSTOM_MSG_TYPE = 11;

    @Test
    public void serverTest() throws Exception {
        ServerConfig serverConfig = new ServerConfig("127.0.0.1", 8080);
        serverConfig.setIoThreads(10);

        var remoteServer = new NettyRemoteServer(serverConfig);
        remoteServer.registerMsgTypeAndHandler(CUSTOM_MSG_TYPE, String.class, new NormalCommunicateTest.EchoMsgHandler());
        remoteServer.init();
        remoteServer.start();

        TimeUnit.MINUTES.sleep(3);
    }

    class EchoMsgHandler implements RemoteMsgHandler{

        @Override
        public RemoteMsg<?> handleMsg(RemoteMsgContext context) {
            RemoteMsg<?> msg = context.getMsg();
            log.info("server received msg：{}", msg.getBody().toString());
            RemoteMsg response = RemoteMsg.createResponse(msg);
            response.setBody("server random: " + new Random().nextInt());
            return response;
        }
    }


    public RemoteClient generateClient(){
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreads(10);
        clientConfig.setQueueSize(1024);
        RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
        remoteClient.init();
        remoteClient.start();
        return remoteClient;
    }

    @Test
    public void clientSendSyncTest() throws Exception {
        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(CUSTOM_MSG_TYPE);
        msg.setSerializeType(InnerSerializer.HESSIAN2.getCode());

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteClient remoteClient = generateClient();
        RemoteMsg<?> responseMsg = remoteClient.send("127.0.0.1:8080", msg, 3100);
        log.info("client received: " + responseMsg.getBody());
        remoteClient.shutdown();
    }

    @Test
    public void clientSendAsyncTest() throws Exception {
        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(CUSTOM_MSG_TYPE);
        msg.setSerializeType(InnerSerializer.HESSIAN2.getCode());

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteClient remoteClient = generateClient();
        CompletableFuture<RemoteMsg<?>> future = remoteClient.sendAsync("127.0.0.1:8080", msg, 3100);
        future.whenComplete(new BiConsumer<RemoteMsg<?>, Throwable>() {
            @Override
            public void accept(RemoteMsg<?> msg, Throwable throwable) {
                log.info("client received: " + msg.getBody());
            }
        });

        TimeUnit.SECONDS.sleep(3);
        remoteClient.shutdown();
    }

}
