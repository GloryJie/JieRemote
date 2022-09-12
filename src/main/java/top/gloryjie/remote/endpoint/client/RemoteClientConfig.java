package top.gloryjie.remote.endpoint.client;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jie-r
 * @since 2022/8/22
 */
@Setter
@Getter
public class RemoteClientConfig {

    private int connectTimeout = 500;

    private int ioThreads = 10;

    private int queueSize = 1024;


}
