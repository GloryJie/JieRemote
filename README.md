
# JieRemote

基于Netty的自有协议远程通信，主要是封装了网络通信中的数据协议层、序列化层。

支持序列化方式的注册、接收不同消息类型。


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

## 客户端

客户端使用RemoteClient进行定义，实现类有NettyRemoteClient。
RemoteClient提供了创建链接、发送消息的能力。

客户端配置如下
```java
public class RemoteClientConfig {
    // 链接超时时间
    private int connectTimeout = 500;
    // io线程数
    private int ioThreads = 8;
    // 如果客户端需要接收服务端的请求，则下面配置会生效
    // 客户端处理消息请求的的线程数
    private int handleMsgThreads = 4;
    // 客户端处理消息请求的队列大小
    private int handleMsgQueueSize = 512;
}
```

客户端的创建
```java
RemoteClientConfig clientConfig = new RemoteClientConfig();
// 客户端io线程数
clientConfig.setIoThreads(10);
RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
// 初始化
remoteClient.init();
// 启动
remoteClient.start();
```

## 服务端

服务端使用RemoteServer表示，其实现类为NettyRemoteServer。

服务端配置
```java
private String bindIp;
private int port;
private int ioThreads;
// 服务端是会接收请求消息进行处理的，如果没有自定义MsgExecutorSelector，则下面配置会生效
private int handleMsgThreads = 10;
private int handleMsgQueueSize = 1024;
private int backlog = 1024;
```

创建服务端
```java
RemoteServerConfig serverConfig = new RemoteServerConfig("127.0.0.1", 8080);
serverConfig.setIoThreads(10);
RemoteServer remoteServer = new NettyRemoteServer(serverConfig);
remoteServer.init();
remoteServer.start();
```

## 客户端和服务端之间的连接

Connection类表示一个链接，只是简单的包装Netty的Channel，可以保存一些KV信息

## 序列化方式

内置了两种序列化方式：JDK自带的序列化、HESSIAN2，在创建RemoteMsg时，通过InnerSerializer枚举获取到对应的code来使用内置的序列化。

自定义序列化方式需要实现接口：ISerializer。

自定义序列化方式，需要在分别在RemoteClient、RemoteServer两端中进行注册，否咋Client发送的序列化类型在Server端无法识别。
```java
RemoteClient.registerSerializer(int type, ISerializer);
RemoteServer.registerSerializer(int type, ISerializer);
```

## 消息类型以及处理器

RemoteMsg中body是泛型，具体的类型未确定，在反序列化的过程中，需要指定具体的类型Class。 并且在接收到对应的请求消息后，需要进行处理，所以需要一个消息处理器。

消息处理器RemoteMsgHandler定义如下
```java
public interface RemoteMsgHandler {
    
    RemoteMsg<?> handleMsg(RemoteMsgContext context);

}
```

为此，一个消息类型需要三个参数：数值代表消息类型、实际的消息类型body承载类Class、对应的消息处理器。 服务端、客户端都提供了注册接口

```java
void registerMsgTypeAndHandler(int msgType, Class<?> bodyType, RemoteMsgHandler handler);
```

通常来说，只有服务端需要接收请求消息进行处理，所以一般只需要服务端进行注册即可。如果有服务端主动向客户端发送请求的情况，客户端也需要进行注册。




## 快速开始使用

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

创建并启动客户端

```java
RemoteClientConfig clientConfig = new RemoteClientConfig();
// 客户端io线程数
clientConfig.setIoThreads(10);
RemoteClient remoteClient = new NettyRemoteClient(clientConfig);
// 初始化
remoteClient.init();
// 启动
remoteClient.start();
```

和server端进行连接，得到一个Connection对象

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
RemoteMsg<?> responseMsg = remoteClient.send(connect, msg, 1000);

// 异步发送
CompletableFuture<RemoteMsg<?>> future = remoteClient.sendAsync(connection, msg, 1000);
future.whenComplete(new BiConsumer<RemoteMsg<?>, Throwable>() {
    @Override
    public void accept(RemoteMsg<?> msg, Throwable throwable) {
        log.info("client received: " + msg.getBody());
    }
});

// 单向发送, 无超时时间
remoteClient.sendOneway(connection, msg);
```

## 规划
- [ ] 链接管理
- [ ] 链接重连机制
- [ ] 心跳机制
- [ ] 服务关闭消息通知
- [ ] 降级为Java8的代码
- [ ] 超时的responseFuture管理
- [ ] responseFuture 纳入到Connection中进行管理