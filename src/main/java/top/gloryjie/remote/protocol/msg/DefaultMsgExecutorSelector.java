package top.gloryjie.remote.protocol.msg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jie-r
 * @since 2022/8/20
 */
public class DefaultMsgExecutorSelector implements MsgExecutorSelector{

    private final ExecutorService executorService;

    public DefaultMsgExecutorSelector(int threadNum, int queueSize) {
        // use LinkedBlockingQueue、AbortPolicy，must catch RejectedExecutionException
        executorService = new ThreadPoolExecutor(threadNum, threadNum,1000, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public ExecutorService select(int msgType) {
        // all msg type use same executor
        return executorService;
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}
