package top.gloryjie.remote.msg;

import java.util.concurrent.ExecutorService;

/**
 * @author jie-r
 * @since 2022/8/16
 */
public interface MsgExecutorSelector {

    /**
     * select one executor by msg type
     * @param msgType
     * @return not null
     */
    ExecutorService select(int msgType);

    /**
     * close all executor
     */
    void close();

}
