package top.gloryjie.remote.connection;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.msg.RemoteMsg;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * combine netty channel and other attr
 *
 * @author jie-r
 * @since 2022/8/15
 */
@Slf4j
@ToString
public class Connection {

    public static final AttributeKey<Connection> CONNECTION_ATTR_KEY = AttributeKey.valueOf("connection");

    private final Channel channel;

    private volatile boolean receivedServerShutDown;

    /**
     * save some attr relate to this channel
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();


    private Connection(Channel channel) {
        this.channel = channel;
        this.channel.attr(CONNECTION_ATTR_KEY).set(this);
    }


    public static Connection getConnection(Channel channel) {
        var connection = channel.attr(CONNECTION_ATTR_KEY).get();
        if (connection == null) {
            synchronized (channel) {
                connection = channel.attr(CONNECTION_ATTR_KEY).get();
                if (connection != null) {
                    return connection;
                }
                connection = new Connection(channel);
            }
        }
        return connection;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) this.channel.remoteAddress();
    }



    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) this.channel.localAddress();
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * @return ip:port
     */
    public String getRemoteAddr() {
        return this.getRemoteAddress().getAddress().getHostAddress() + ":" + this.getRemoteAddress().getPort();
    }

    public String getLocalAddr(){
        InetSocketAddress localAddress = this.getLocalAddress();
        return localAddress.getHostName() + ":" + localAddress.getPort();
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * close channel and log
     */
    public void close() {
        String remoteStr = this.getRemoteAddr();
        this.channel.close().addListener((future) ->
                log.info("[JieRemote][server][Connection#close]remoteStr={}, closeResult={}", remoteStr, future.isSuccess()));
    }

    public boolean isActive(){
        return this.channel.isActive() && !receivedServerShutDown;
    }


    public CompletableFuture<Void> send(RemoteMsg<?> msg){
        CompletableFuture<Void> sendResultFuture = new CompletableFuture<>();
        getChannel().writeAndFlush(msg).addListener(future -> {
            if (future.isSuccess()) {
                sendResultFuture.complete(null);
            } else {
                sendResultFuture.completeExceptionally(future.cause());
            }
        });
        return sendResultFuture;
    }

    public void setReceivedServerShutDown(boolean receivedServerShutDown) {
        this.receivedServerShutDown = receivedServerShutDown;
    }
}
