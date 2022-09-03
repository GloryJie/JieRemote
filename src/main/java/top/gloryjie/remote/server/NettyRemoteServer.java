package top.gloryjie.remote.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.connection.Connection;
import top.gloryjie.remote.endpoint.AbstractRemote;
import top.gloryjie.remote.endpoint.RemoteServer;
import top.gloryjie.remote.exception.ExceptionProtocol;
import top.gloryjie.remote.exception.RemoteException;
import top.gloryjie.remote.msg.*;
import top.gloryjie.remote.protocol.RemoteMsgDecoder;
import top.gloryjie.remote.protocol.RemoteMsgEncoder;
import top.gloryjie.remote.serializer.ISerializer;
import top.gloryjie.remote.serializer.InnerSerializer;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author jie-r
 * @since 2022/8/15
 */
@Slf4j
public class NettyRemoteServer extends AbstractRemote implements RemoteServer {

    // config & netty component
    private final ServerConfig serverConfig;
    private final NioEventLoopGroup bossThreads;
    private final NioEventLoopGroup workerThreads;
    private final ServerBootstrap serverBootstrap;

    // handler
    private RemoteMsgEncoder remoteMsgEncoder;
    private NettyConnectionManageHandler connectionManageHandler;
    private NettyServerMsgHandler serverMsgHandler;

    public NettyRemoteServer(ServerConfig serverConfig) {
        this(serverConfig, new DefaultMsgExecutorSelector(serverConfig.getBizThreads(), serverConfig.getBizQueueSize()));
    }

    public NettyRemoteServer(ServerConfig serverConfig, MsgExecutorSelector executorSelector) {
        super(executorSelector);
        this.serverConfig = serverConfig;
        serverBootstrap = new ServerBootstrap();
        bossThreads = new NioEventLoopGroup(1, new DefaultThreadFactory("serverBossWorker", true));
        var ioThreads = serverConfig.getIoThreads() > 0 ? serverConfig.getIoThreads() : serverConfig.getDefaultIoThreads();
        workerThreads = new NioEventLoopGroup(ioThreads, new DefaultThreadFactory("serverIoWorker", true));
        try {
            // init thread to run
            bossThreads.invokeAll(Collections.singletonList((Callable<Void>) () -> null));
            workerThreads.invokeAll(IntStream.range(0, ioThreads).mapToObj(i -> (Callable<Void>) () -> null).collect(Collectors.toList()));
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void init() {
        // create netty handler
        this.remoteMsgEncoder = new RemoteMsgEncoder();
        this.connectionManageHandler = new NettyConnectionManageHandler();
        this.serverMsgHandler = new NettyServerMsgHandler();

        // register default serializer
        registerInnerSerializer();

        // register default msg
        registerDefaultMsgType();
    }

    @Override
    public void start() {
        serverBootstrap.group(this.bossThreads, this.workerThreads)
                .channel(NioServerSocketChannel.class)
                // new connection queue size (before accept)
                .option(ChannelOption.SO_BACKLOG, serverConfig.getBacklog())
                // avoid port occupy
                .option(ChannelOption.SO_REUSEADDR, true)
                // ban nagle
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("encoder", remoteMsgEncoder);
                        pipeline.addLast("decoder", new RemoteMsgDecoder());
                        pipeline.addLast(connectionManageHandler);
                        pipeline.addLast(serverMsgHandler);
                    }
                });

        var future = serverBootstrap.bind(serverConfig.getBindIp(), serverConfig.getPort()).syncUninterruptibly();
        if (future.isSuccess()) {
            log.info("[JieRemote][Server][start] start success, ip={}, port={}, ioThreads={}", serverConfig.getBindIp(), serverConfig.getPort(), serverConfig.getIoThreads());
        }
    }

    @Override
    public void shutdown() {
        this.bossThreads.shutdownGracefully();
        this.workerThreads.shutdownGracefully();
        this.executorSelector.close();
    }

    @Override
    public void registerMsgTypeAndHandler(int msgType, Class<?> bodyType, RemoteMsgHandler handler) {
        if (msgType < 10){
            throw new IllegalArgumentException("msgType must greater than 10");
        }
        msgTypeMap.put(msgType, bodyType);
        msgHandlerMap.put(msgType, handler);
    }

    @Override
    public void registerExecutorSelector(MsgExecutorSelector selector) {
        executorSelector = selector;
    }

    @Override
    public void registerSerializer(int type, ISerializer serializer) {
        if (type < 11) {
            throw new IllegalArgumentException("serializerType must greater than 10");
        }
        serializerMap.put(type, serializer);
    }


    public void sendExceptionResp(Connection connection, int msgId, ExceptionProtocol exceptionProtocol) {
        RemoteMsg<ExceptionProtocol> respMsg = new RemoteMsg<>();
        respMsg.setSerializeType(InnerSerializer.HESSIAN2.getCode());
        respMsg.setCompressType(RemoteMsg.COMPRESS_TYPE_NONE);
        respMsg.setMsgType(InnerMsgType.REMOTE_EXCEPTION.getMsgType());
        respMsg.setMsgId(msgId);

        respMsg.setBody(exceptionProtocol);

        try {
            serializeRemoteMsg(respMsg);
        } catch (Exception e) {
            // ignore
            log.error("[JieRemote][Server][sendExceptionProtocol] serialize err", e);
            return;
        }
        connection.getChannel().writeAndFlush(respMsg);
    }


    @ChannelHandler.Sharable
    class NettyServerMsgHandler extends SimpleChannelInboundHandler<RemoteMsg<?>> {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteMsg<?> remoteMsg) throws Exception {
            var connection = Connection.getConnection(channelHandlerContext.channel());

            // deserialize header and body
            var serializer = serializerMap.get(remoteMsg.getSerializeType());
            if (serializer == null && remoteMsg.isRequest()) {
                log.error("[JieRemote][Server][processMsgReceive] can't find serializer,remote={},msgId={},msgType={}",
                        connection.getRemoteAddr(), remoteMsg.getMsgId(), remoteMsg.getMsgType());
                var exceptionProtocol = new ExceptionProtocol();
                exceptionProtocol.setErrorCode(RemoteException.SERVER_DESERIALIZE_ERR);
                exceptionProtocol.setErrorMsg(String.format("server can't found this msg type serializer, serverIp=%s, msgType=%s", connection.getLocalAddr(), remoteMsg.getMsgType()));
                sendExceptionResp(connection, remoteMsg.getMsgId(), exceptionProtocol);
                return;
            }

            try {
                remoteMsg.deserialize(serializer, msgTypeMap.get(remoteMsg.getMsgType()));
            } catch (Exception e) {
                log.error("[JieRemote][Server][processMsgReceive] deserialize msg fail,remote={},serializerType={},msgId={},msgType={}",
                        connection.getRemoteAddr(), remoteMsg.getSerializeType(), remoteMsg.getMsgId(), remoteMsg.getMsgType(), e);
                if (remoteMsg.isRequest()) {
                    ExceptionProtocol exceptionProtocol = new ExceptionProtocol();
                    exceptionProtocol.setErrorCode(RemoteException.SERVER_DESERIALIZE_ERR);
                    exceptionProtocol.setErrorMsg(String.format("server deserialize exception, serverIp=%s, msgType=%s,errMsg=%s", connection.getLocalAddr(), remoteMsg.getMsgType(), e.getMessage()));
                    sendExceptionResp(connection, remoteMsg.getMsgId(), exceptionProtocol);
                }
                return;
            }

            if (remoteMsg.isResponse()) {
                CompletableFuture<RemoteMsg<?>> responseFuture = responseFutureMap.remove(remoteMsg.getMsgId());
                if (responseFuture == null) {
                    log.warn("[JieRemote][Server][processMsgReceive] can't not find responseFuture msgId={} msgType={},remoteStr={}", remoteMsg.getMsgId(), remoteMsg.getMsgType(), connection.getRemoteAddr());
                    return;
                }
                responseFuture.complete(remoteMsg);
                return;
            } else {
                // select msg handle executor
                EventExecutor eventExecutor = connection.getChannel().eventLoop();
                ExecutorService msgHandlerExecutor = executorSelector.select(remoteMsg.getMsgType());

                RemoteMsgHandler remoteMsgHandler = msgHandlerMap.get(remoteMsg.getMsgType());
                if (remoteMsgHandler == null) {
                    log.error("[JieRemote][Server][processRequestMsg] can't find msg handler, msgType={}, remoteStr={}", remoteMsg.getMsgType(), connection.getRemoteAddr());
                    var exceptionProtocol = new ExceptionProtocol();
                    exceptionProtocol.setErrorCode(RemoteException.SERVER_NOT_SUPPORT_MSG_TYPE_ERR);
                    exceptionProtocol.setErrorMsg(String.format("server can't found this msg type handler, serverIp=%s, msgType=%s", connection.getLocalAddr(), remoteMsg.getMsgType()));
                    sendExceptionResp(connection, remoteMsg.getMsgId(), exceptionProtocol);
                    return;
                }

                // create msg context
                RemoteMsgContext msgContext = new RemoteMsgContext();
                msgContext.setMsg(remoteMsg);

                // handle msg use msgHandlerExecutor
                CompletableFuture
                        .supplyAsync(() -> remoteMsgHandler.handleMsg(msgContext), msgHandlerExecutor)
                        .whenCompleteAsync((respMsg, err) -> {
                            if (err != null) {
                                log.error("[JieRemote][Server][processRequestMsg] request msg handle fail msgId={}, msgType={}", remoteMsg.getMsgId(), remoteMsg.getMsgType(), err);
                            }
                            if (!remoteMsg.isOneway()) {
                                if (err == null) {
                                    log.debug("[JieRemote][processRequestMsg] req msg handle success msgId={}, msgType={}", remoteMsg.getMsgId(), remoteMsg.getMsgType());

                                    // same with reqMsg
                                    respMsg.setSerializeType(remoteMsg.getSerializeType());
                                    respMsg.setCompressType(remoteMsg.getCompressType());
                                    respMsg.setMsgId(remoteMsg.getMsgId());
                                    respMsg.setMsgType(remoteMsg.getMsgType());
                                    respMsg.markRespFlag();

                                    // serial resp msg
                                    try {
                                        serializeRemoteMsg(respMsg);
                                    } catch (Exception e) {
                                        ExceptionProtocol exceptionProtocol = new ExceptionProtocol();
                                        exceptionProtocol.setErrorCode(RemoteException.SERVER_NOT_SUPPORT_MSG_TYPE_ERR);
                                        exceptionProtocol.setErrorMsg(String.format("server can't found this msg type handler, serverIp=%s, msgType=%s", connection.getLocalAddr(), remoteMsg.getMsgType()));
                                        sendExceptionResp(connection, remoteMsg.getMsgId(), exceptionProtocol);
                                        return;
                                    }

                                    // write response msg back
                                    connection.getChannel().writeAndFlush(respMsg).addListener(future -> {
                                        if (future.isSuccess()) {
                                            log.debug("[JieRemote][processRequestMsg][processRequestMsg] write response success, msgId={}, msgType={}", remoteMsg.getMsgId(), remoteMsg.getMsgType());
                                        } else {
                                            log.error("[JieRemote][processRequestMsg][processRequestMsg] write response fail, msgId={}, msgType={}", remoteMsg.getMsgId(), remoteMsg.getMsgType(), future.cause());
                                        }
                                    });
                                } else {
                                    ExceptionProtocol exceptionProtocol = new ExceptionProtocol();
                                    if (err instanceof RejectedExecutionException) {
                                        exceptionProtocol.setErrorCode(RemoteException.SERVER_EXECUTOR_ABORT);
                                        exceptionProtocol.setErrorMsg(String.format("server reject this req, serverIp=%s, msgType=%s", connection.getLocalAddr(), remoteMsg.getMsgType()));
                                    } else {
                                        exceptionProtocol.setErrorCode(RemoteException.SERVER_HANDLER_ERR);
                                        exceptionProtocol.setErrorMsg(String.format("server handle this msg fail, serverIp=%s, msgType=%s, errMsg=%s", connection.getLocalAddr(), remoteMsg.getMsgType(), err.getMessage()));
                                    }
                                    sendExceptionResp(connection, remoteMsg.getMsgId(), exceptionProtocol);
                                    return;
                                }
                            }
                        }, eventExecutor);
            }
        }

    }

    @ChannelHandler.Sharable
    class NettyConnectionManageHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Connection connection = Connection.getConnection(ctx.channel());
            log.info("[JieRemote][Server][connected]remoteStr={}. connection={}", connection.getRemoteAddr(), connection);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Connection connection = Connection.getConnection(ctx.channel());
            log.info("[JieRemote][Server][disConnected]remoteStr={}. connection={}", connection.getRemoteAddr(), connection);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Connection connection = Connection.getConnection(ctx.channel());
            log.warn("[JieRemote][Server][exceptionCaught]remoteStr={},connection={}", connection.getRemoteAddr(), connection, cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            // TODO handle event
            super.userEventTriggered(ctx, evt);
        }

    }
}
