package top.gloryjie.remote.endpoint.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.connection.Connection;
import top.gloryjie.remote.endpoint.AbstractRemote;
import top.gloryjie.remote.endpoint.RemoteClient;
import top.gloryjie.remote.exception.ExceptionProtocol;
import top.gloryjie.remote.exception.RemoteException;
import top.gloryjie.remote.protocol.RemoteMsgDecoder;
import top.gloryjie.remote.protocol.RemoteMsgEncoder;
import top.gloryjie.remote.protocol.msg.*;
import top.gloryjie.remote.serializer.ISerializer;
import top.gloryjie.remote.util.RemoteUtil;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * one client can hold one or more connection
 *
 * @author jie-r
 * @since 2022/8/20
 */
@Slf4j
public class NettyRemoteClient extends AbstractRemote implements RemoteClient {

    private final RemoteClientConfig clientConfig;
    private final EventLoopGroup workerGroup;
    private final Bootstrap bootstrap;

    // netty handler
    private RemoteMsgEncoder remoteMsgEncoder;
    private ClientConnectionManagerHandler connectionManagerHandler;
    private NettyClientMsgHandler clientMsgHandler;

    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    Lock connectionLock = new ReentrantLock();


    public NettyRemoteClient(RemoteClientConfig clientConfig) {
        this(clientConfig, new DefaultMsgExecutorSelector(clientConfig.getIoThreads(), clientConfig.getQueueSize()));
    }

    public NettyRemoteClient(RemoteClientConfig clientConfig, MsgExecutorSelector executorSelector) {
        super(executorSelector);
        this.clientConfig = clientConfig;
        bootstrap = new Bootstrap();
        workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("clientGroup"));
    }


    @Override
    public void registerMsgTypeAndHandler(int msgType, Class<?> bodyType, RemoteMsgHandler handler) {
        msgTypeMap.put(msgType, bodyType);
        msgHandlerMap.put(msgType, handler);
    }

    @Override
    public void registerSerializer(int type, ISerializer serializer) {
        if (type < 11) {
            throw new IllegalArgumentException("serializerType must greater than 10");
        }
        serializerMap.put(type, serializer);
    }

    @Override
    public RemoteMsg<?> send(Connection connection, RemoteMsg<?> msg, long timeoutMillis) {
        var responseFuture = new CompletableFuture<RemoteMsg<?>>();

        try {
            serializeRemoteMsg(msg);
        } catch (Exception e) {
            log.error("[JieRemote][sendSyncImpl]serialize msg err, msgType={}, serializeType={}", msg.getMsgType(), msg.getSerializeType(), e);
            throw new RemoteException(RemoteException.CLIENT_SERIALIZE_ERR, "send sync serialize err: " + e.getMessage());
        }
        // cache this msg responseFuture
        responseFutureMap.put(msg.getMsgId(), responseFuture);
        connection.send(msg).whenComplete((Void, throwable) -> {
            if (throwable != null) {
                log.error("[JieRemote][Client][sendSyncImpl] send err, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
                // send fail
                if (throwable instanceof EncoderException) {
                    responseFuture.completeExceptionally(new RemoteException(RemoteException.CLIENT_ENCODE_ERR, throwable));
                } else {
                    responseFuture.completeExceptionally(new RemoteException(RemoteException.CLIENT_WRITE_DATA_ERR, throwable));
                }
                // rm future from connection's
                responseFutureMap.remove(msg.getMsgId());
                RemoteException exception = null;
                if (throwable instanceof RemoteException e) {
                    exception = e;
                } else {
                    exception = new RemoteException(RemoteException.CLIENT_WRITE_DATA_ERR, throwable);
                }
                responseFuture.completeExceptionally(exception);
            } else {
                // send success
                log.debug("[JieRemote][Client][sendSync] send success, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
            }
        });

        // handle&transfer Exception
        try {
            return responseFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RemoteException(RemoteException.CLIENT_REQ_TIMEOUT, "request timeout msgId=" + msg.getMsgId(), e);
        } catch (RemoteException e) {
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RemoteException remoteException) {
                throw remoteException;
            } else {
                throw new RemoteException(RemoteException.OTHER_ERR, e);
            }
        } catch (Throwable e) {
            throw new RemoteException(RemoteException.OTHER_ERR, e);
        }
    }

    @Override
    public CompletableFuture<RemoteMsg<?>> sendAsync(Connection connection, RemoteMsg<?> msg, long timeoutMillis) {
        var responseFuture = new CompletableFuture<RemoteMsg<?>>();

        RemoteException remoteException = null;

        try {
            serializeRemoteMsg(msg);
        } catch (Exception e) {
            log.error("[JieRemote][Client][sendSyncImpl]serialize msg err, msgType={}, serializeType={}", msg.getMsgType(), msg.getSerializeType(), e);
            remoteException = new RemoteException(RemoteException.CLIENT_SERIALIZE_ERR, "send sync serialize err", e);
        }

        if (remoteException != null) {
            responseFuture.completeExceptionally(remoteException);
        } else {
            responseFutureMap.put(msg.getMsgId(), responseFuture);
            connection.send(msg).whenComplete((Void, throwable) -> {
                if (throwable != null) {
                    log.error("[JieRemote][sendAsync] send err, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
                    // send fail
                    if (throwable instanceof EncoderException) {
                        responseFuture.completeExceptionally(new RemoteException(RemoteException.CLIENT_ENCODE_ERR, throwable));
                    } else {
                        responseFuture.completeExceptionally(new RemoteException(RemoteException.CLIENT_WRITE_DATA_ERR, throwable));
                    }
                    // rm future from connection's
                    responseFutureMap.remove(msg.getMsgId());
                    RemoteException exception = null;
                    if (throwable instanceof RemoteException e) {
                        exception = e;
                    } else {
                        exception = new RemoteException(RemoteException.CLIENT_WRITE_DATA_ERR, throwable);
                    }
                    responseFuture.completeExceptionally(exception);
                } else {
                    // send success
                    log.debug("[JieRemote][sendSyncImpl] send success, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
                }
            });
        }
        // TODO timeout rm responseFuture
        return responseFuture.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void sendOneway(Connection connection, RemoteMsg<?> msg, long timeoutMillis) {
        connection.send(msg).whenComplete((Void, throwable) -> {
            if (throwable == null) {
                log.debug("[JieRemote][sendOneway] send success, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
            } else {
                log.error("[JieRemote][sendOneway] send err, msgId={} msgType={},remoteStr={}", msg.getMsgId(), msg.getMsgType(), connection.getRemoteAddr());
            }
        });
    }

    @Override
    public Connection connect(String addr, long timeoutMills) {
        // create new connection
        var future = this.bootstrap.connect(RemoteUtil.string2SocketAddress(addr));
        future.awaitUninterruptibly();
        if (future.cause() != null) {
            log.warn("[JieRemote][client][connect] connect remote host fail, addr={}", addr, future.cause());
            throw new RemoteException(RemoteException.CLIENT_CONNECT_ERR, future.cause());
        }
        var connection = Connection.getConnection(future.channel());
        log.info("[JieRemote][client][connect] connect remote host success, addr={}, connection={}", addr, connection);
        return connection;
    }

    @Override
    public void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        connection.close();
    }

    @Override
    public void init() {
        remoteMsgEncoder = new RemoteMsgEncoder();
        connectionManagerHandler = new ClientConnectionManagerHandler();
        clientMsgHandler = new NettyClientMsgHandler();

        // register default serializer
        registerInnerSerializer();
        registerDefaultMsgType();
    }

    @Override
    public void start() {
        this.bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfig.getConnectTimeout())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("encoder", remoteMsgEncoder);
                        pipeline.addLast("decoder", new RemoteMsgDecoder());
                        pipeline.addLast(connectionManagerHandler);
                        pipeline.addLast(clientMsgHandler);
                    }
                });
    }

    @Override
    public void shutdown() {
        workerGroup.shutdownGracefully();
        executorSelector.close();
    }

    @ChannelHandler.Sharable
    class NettyClientMsgHandler extends SimpleChannelInboundHandler<RemoteMsg<?>> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteMsg<?> remoteMsg) throws Exception {
            Channel channel = channelHandlerContext.channel();
            Connection connection = Connection.getConnection(channel);

            // deserialize header and body
            ISerializer serializer = serializerMap.get(remoteMsg.getSerializeType());
            if (serializer == null) {
                log.error("[JieRemote][Client][processMsgReceive] can't find serializer,remote={},msgId={},msgType={}",
                        connection.getRemoteAddr(), remoteMsg.getMsgId(), remoteMsg.getMsgType());
                CompletableFuture<RemoteMsg<?>> responseFuture = responseFutureMap.remove(remoteMsg.getMsgId());
                if (responseFuture != null) {
                    RemoteException remoteException = new RemoteException(RemoteException.CLIENT_DESERIALIZE_ERR, "can't find serializer for this SerializeType=" + remoteMsg.getSerializeType());
                    responseFuture.completeExceptionally(remoteException);
                    return;
                }
            }

            try {
                remoteMsg.deserialize(serializer, msgTypeMap.get(remoteMsg.getMsgType()));
            } catch (Exception e) {
                log.error("[JieRemote][Client][processMsgReceive] deserialize msg fail,remote={},msgId={},msgType={}",
                        connection.getRemoteAddr(), remoteMsg.getMsgId(), remoteMsg.getMsgType(), e);
                CompletableFuture<RemoteMsg<?>> responseFuture = responseFutureMap.remove(remoteMsg.getMsgId());
                if (responseFuture != null) {
                    RemoteException remoteException = new RemoteException(RemoteException.CLIENT_DESERIALIZE_ERR, "deserialize exception" + remoteMsg.getSerializeType(), e);
                    responseFuture.completeExceptionally(remoteException);
                    return;
                }
            }

            // inner msg handle
            InnerMsgType innerMsgType = InnerMsgType.getByCode(remoteMsg.getMsgType());
            if (innerMsgType != null){
                switch (innerMsgType){
                    case SERVER_SHUTDOWN -> connection.setReceivedServerShutDown(true);
                    case REMOTE_EXCEPTION -> {
                        if (remoteMsg.getBody() instanceof ExceptionProtocol exceptionProtocol){
                            CompletableFuture<RemoteMsg<?>> responseFuture = responseFutureMap.remove(remoteMsg.getMsgId());
                            if (responseFuture == null){
                                log.warn("[JieRemote][Client][processMsgReceive] can't found response future, msgId={}", remoteMsg.getMsgId());
                                return;
                            }
                            RemoteException remoteException = new RemoteException(exceptionProtocol.getErrorCode(), exceptionProtocol.getErrorMsg());
                            responseFuture.completeExceptionally(remoteException);
                        }

                    }
                }
                return;
            }



            if (remoteMsg.isResponse()){
                CompletableFuture<RemoteMsg<?>> responseFuture = responseFutureMap.remove(remoteMsg.getMsgId());
                if (responseFuture == null){
                    log.warn("[JieRemote][Client][processMsgReceive] can't found response future, msgId={}", remoteMsg.getMsgId());
                    return;
                }
                responseFuture.complete(remoteMsg);
            }

        }
    }


    public Connection getAndCreateConnection(String addr) {
        var connection = connectionMap.get(addr);
        if (connection == null) {
            try {
                if (connectionLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                    connection = connectionMap.get(addr);
                    if (connection != null) {
                        if (connection.isActive()) {
                            return connection;
                        } else {
                            connectionMap.remove(addr);
                        }
                    }

                    // create new connection
                    var future = this.bootstrap.connect(RemoteUtil.string2SocketAddress(addr));
                    future.awaitUninterruptibly();
                    if (future.cause() != null) {
                        log.warn("[JieRemote][client][getAndCreateConnection] connect remote host fail, addr={}, connection={}", addr, connection, future.cause());
                        throw new RemoteException(RemoteException.CLIENT_CONNECT_ERR, future.cause());
                    }
                    log.info("[JieRemote][client][getAndCreateConnection] connect remote host success, addr={}, connection={}", addr, connection);
                    connection = Connection.getConnection(future.channel());
                    connectionMap.put(addr, connection);
                    return connection;
                } else {
                    log.info("[JieRemote][client][getAndCreateConnection] try lock timeout, addr={}", addr);
                }
            } catch (InterruptedException e) {
                log.error("[JieRemote][client][getAndCreateConnection] try lock had been interrupted, addr={}", addr, e);

            } finally {
                connectionLock.unlock();
            }
        }

        return null;
    }

}
