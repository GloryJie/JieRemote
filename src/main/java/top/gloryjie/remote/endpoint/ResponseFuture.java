package top.gloryjie.remote.endpoint;

import lombok.Getter;
import lombok.Setter;
import top.gloryjie.remote.exception.RemoteException;
import top.gloryjie.remote.msg.RemoteMsg;

import java.util.concurrent.CompletableFuture;

/**
 * @author jie-r
 * @since 2022/8/18
 */
@Getter
@Setter
public class ResponseFuture {

    private Integer msgId;
    private volatile RemoteMsg<?> responseMsg;
    private volatile boolean sendRequestOK = true;
    private RemoteException remoteException;
    private volatile Throwable cause;
    private CompletableFuture<RemoteMsg<?>> future;



}
