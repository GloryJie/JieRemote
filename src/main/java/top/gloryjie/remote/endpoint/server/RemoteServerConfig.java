package top.gloryjie.remote.endpoint.server;

import lombok.Getter;
import lombok.Setter;

/**
 * server config model
 * @author jie-r
 * @since 2022/8/15
 */
@Setter
@Getter
public class RemoteServerConfig {

    private String bindIp;

    private int port;

    private int ioThreads;

    private int handleMsgThreads = 10;

    private int handleMsgQueueSize = 1024;

    private int backlog = 1024;

    public RemoteServerConfig(String bindIp, int port) {
        this.bindIp = bindIp;
        this.port = port;
        // default use availableProcessors
        ioThreads = getDefaultIoThreads();
    }

    public RemoteServerConfig(String bindIp, int port, int ioThreads) {
        this.bindIp = bindIp;
        this.port = port;
        this.ioThreads = ioThreads;
    }

    public int getDefaultIoThreads(){
        return Runtime.getRuntime().availableProcessors();
    }

}
