
# JieRemote

基于Netty的自有协议远程通信，主要是封装了网络通信中的协议层、序列化层。

提供不同序列化方式的注册、不同消息承载类型的注册


## 自定义协议
协议内容如下

| 字段 | 字节数 | 描述 |
| --- | --- | --- |
| 魔数 | 2byte | 0xd8 |
| 总长度 | 4byte |  |
| 头部长度 | 2byte |  |
| 版本号 | 1byte | 预留位，默认1 |
| 序列化方式 | 1byte |
| 压缩方式 | 1byte | 预留位，默认0 |
| 消息类型 | 2byte |  |
| 消息id | 4byte |  |
| 标记位 | 1byte | 用于表示请求、响应、单向 |
| 头部扩展部分 | 头部长度-12 | 直接使用HashMap<String,String>来表示 |
| body | 总长度-头部长度 |  |

协议的具体内容使用类RemoteMsg来表示

消息的编解码参见：RemoteMsgEncoder、RemoteMsgDecoder

## 消息的流转处理

RemoteClient发送消息后，会在Netty的IO线程中完成序列化操作，之后将RemoteMsg发送到远端。

RemoteServer接收到消息后，在Netty的IO线程完成反序列化操作，之后通过MsgExecutorSelector根据消息类型选择出Executor进行消息的处理。


## 使用

可以参考类NormalCommunicateTest


### 创建消息类型以及对应的处理器

这里直接使用String作为消息的body类型，其类型数值为11，处理器为EchoMsgHandler

```java
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
```

### 服务端

初始化服务端，需要注册消息类型和对应的处理器

```java
ServerConfig serverConfig = new ServerConfig("127.0.0.1", 8080);
serverConfig.setIoThreads(10);

var remoteServer = new NettyRemoteServer(serverConfig);
// 这里进行消息类型和对应处理器的注册
remoteServer.registerMsgTypeAndHandler(11, String.class, EchoMsgHandler());
remoteServer.init();

```

进行启动

```java
remoteServer.start();
```

### 客户端

创建一个和链接无关的客户端

```java
RemoteClientConfig clientConfig = new RemoteClientConfig();
// 客户端io线程数
clientConfig.setIoThreads(10);
// 客户端的队列大小
clientConfig.setQueueSize(1024);
RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
// 初始化
remoteClient.init();
// 启动
remoteClient.start();
```

和server端进行链接，得到一个Connection链接对象

```
Connection connect = remoteClient.connect("127.0.0.1:8080", 3100);
```

生成一个消息，指定消息类型为11，序列化方式为内置的HESSIAN2
```java
RemoteMsg<String> msg =  (RemoteMsg<String>) RemoteMsg.createRequest();
msg.setMsgType(11);
msg.setSerializeType(InnerSerializer.HESSIAN2.getCode());

HashMap<String, String> header = new HashMap<>();
header.put("timestamp", String.valueOf(System.currentTimeMillis()));
msg.setHeaderExt(header);
msg.setBody("hello server");
```

进行发送

```java
// 同步发送，参数分别为：指定发送的链接、消息、超时时间
RemoteMsg<?> responseMsg = remoteClient.send(connect, msg, 3100);

// 异步发送
CompletableFuture<RemoteMsg<?>> future = remoteClient.sendAsync(connection, msg, 3100);
future.whenComplete(new BiConsumer<RemoteMsg<?>, Throwable>() {
    @Override
    public void accept(RemoteMsg<?> msg, Throwable throwable) {
        log.info("client received: " + msg.getBody());
    }
});

// 单向发送
remoteClient.sendOneway(connection, msg, 3100);
```

## 规划
- [ ] 链接管理
- [ ] 链接重连机制
- [ ] 心跳机制
- [ ] 服务关闭消息通知
- [ ] 降级为Java8的代码
- [ ] 超时的responseFuture管理
- [ ] responseFuture 纳入到Connection中进行管理