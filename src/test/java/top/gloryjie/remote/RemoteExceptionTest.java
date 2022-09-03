package top.gloryjie.remote;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.gloryjie.remote.endpoint.client.ClientConfig;
import top.gloryjie.remote.endpoint.client.NettyRemoteClient;
import top.gloryjie.remote.endpoint.RemoteClient;
import top.gloryjie.remote.protocol.msg.RemoteMsg;
import top.gloryjie.remote.protocol.msg.RemoteMsgContext;
import top.gloryjie.remote.protocol.msg.RemoteMsgHandler;
import top.gloryjie.remote.serializer.Hessian2Serializer;
import top.gloryjie.remote.serializer.ISerializer;
import top.gloryjie.remote.serializer.InnerSerializer;
import top.gloryjie.remote.endpoint.server.NettyRemoteServer;
import top.gloryjie.remote.endpoint.server.ServerConfig;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author jie-r
 * @since 2022/9/3
 */
@Slf4j
public class RemoteExceptionTest {

    int customSerializerType = 11;
    int exceptionSerializer = 12;

    int normalMsgType = 11;

    @Test
    public void serverExceptionTest() throws Exception{
        ServerConfig serverConfig = new ServerConfig("127.0.0.1", 8080);
        serverConfig.setIoThreads(10);

        var remoteServer = new NettyRemoteServer(serverConfig);
        remoteServer.registerMsgTypeAndHandler(normalMsgType, String.class, new RemoteExceptionTest.CustomExceptionHandler());
        remoteServer.registerSerializer(exceptionSerializer, new ISerializer() {
            @Override
            public byte[] writeHeader(HashMap<String, String> headerMap) throws Exception {
                throw new RuntimeException("serial err test");
            }

            @Override
            public HashMap<String, String> readHeader(byte[] data) throws Exception {
                throw new RuntimeException("serial err test");
            }

            @Override
            public byte[] writeBody(Object obj) throws Exception {
                throw new RuntimeException("serial err test");
            }

            @Override
            public Object readBody(byte[] data, Class<?> cls) throws Exception {
                throw new RuntimeException("serial err test");
            }
        });
        remoteServer.init();
        remoteServer.start();

        TimeUnit.MINUTES.sleep(3);

    }


    @Test
    public void serializerNotFoundExceptionTest() throws Exception{
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreads(10);
        clientConfig.setQueueSize(1024);

        RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
        remoteClient.registerSerializer(exceptionSerializer, new Hessian2Serializer());
        remoteClient.init();
        remoteClient.start();

        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(1);
        msg.setSerializeType(exceptionSerializer);

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteMsg<?> responseMsg = remoteClient.send("127.0.0.1:8080", msg, 3100);
        System.out.println("client received: " + responseMsg.getBody());

        TimeUnit.SECONDS.sleep(10);

        remoteClient.shutdown();
    }

    @Test
    public void serverDeserializerExceptionTest() throws Exception{
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreads(10);
        clientConfig.setQueueSize(1024);

        RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
        remoteClient.registerSerializer(exceptionSerializer, new Hessian2Serializer());

        remoteClient.init();
        remoteClient.start();

        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(1);
        msg.setSerializeType(exceptionSerializer);

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteMsg<?> responseMsg = remoteClient.send("127.0.0.1:8080", msg, 3100);
        System.out.println("client received: " + responseMsg.getBody());

        TimeUnit.SECONDS.sleep(10);

        remoteClient.shutdown();
    }

    @Test
    public void handlerNotFoundTest() throws Exception{
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreads(10);
        clientConfig.setQueueSize(1024);

        RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
        remoteClient.init();
        remoteClient.start();

        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(20);
        msg.setSerializeType(InnerSerializer.HESSIAN2.getCode());

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteMsg<?> responseMsg = remoteClient.send("127.0.0.1:8080", msg, 3100);
        System.out.println("client received: " + responseMsg.getBody());

        TimeUnit.SECONDS.sleep(10);

        remoteClient.shutdown();
    }

    @Test
    public void handlerExceptionTest() throws Exception{
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreads(10);
        clientConfig.setQueueSize(1024);

        RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
        remoteClient.init();
        remoteClient.start();

        RemoteMsg<String> msg = new RemoteMsg<>();
        msg.setMsgType(normalMsgType);
        msg.setSerializeType(InnerSerializer.HESSIAN2.getCode());

        HashMap<String, String> header = new HashMap<>();
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.setHeaderExt(header);
        msg.setBody("hello server");

        RemoteMsg<?> responseMsg = remoteClient.send("127.0.0.1:8080", msg, 3100);
        System.out.println("client received: " + responseMsg.getBody());

        TimeUnit.SECONDS.sleep(10);

        remoteClient.shutdown();
    }



    class CustomExceptionHandler implements RemoteMsgHandler {

        @Override
        public RemoteMsg<?> handleMsg(RemoteMsgContext context) {
            var msg = context.getMsg();
            log.info("[JieRemote][CustomExceptionHandler] receive msg,header={},body={}", msg.getHeaderExt(), msg.getBody());
            throw new RuntimeException("test handler Exception");
        }
    }


}
