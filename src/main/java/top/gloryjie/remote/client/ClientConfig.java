package top.gloryjie.remote.client;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jie-r
 * @since 2022/8/22
 */
@Setter
@Getter
public class ClientConfig {

    private String host;

    private int port;

    private int connectTimeout = 500;

    private int ioThreads = 10;

    private int queueSize = 1024;


}
