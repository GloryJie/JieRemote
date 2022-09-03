package top.gloryjie.remote.endpoint.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.connection.Connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO Deprecated this handler
 * @author jie-r
 * @since 2022/8/21
 */
@Slf4j
public class ClientConnectionManagerHandler extends ChannelInboundHandlerAdapter {

    private final Map<String, Connection> CONNECTION_MAP = new ConcurrentHashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = Connection.getConnection(ctx.channel());
        log.info("[JieRemote][client][channelActive]remoteStr={}. connection={}", connection.getRemoteAddr(), connection);
        CONNECTION_MAP.put(connection.getRemoteAddr(), connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = Connection.getConnection(ctx.channel());
        log.info("[JieRemote][client][channelInactive]remoteStr={}. connection={}", connection.getRemoteAddr(), connection);
        CONNECTION_MAP.remove(connection.getRemoteAddr());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Connection connection = Connection.getConnection(ctx.channel());
        log.warn("[JieRemote][client][exceptionCaught]remoteStr={}. connection={}", connection.getRemoteAddr(), connection);
        connection.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

}
