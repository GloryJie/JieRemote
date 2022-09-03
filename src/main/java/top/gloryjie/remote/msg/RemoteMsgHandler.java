package top.gloryjie.remote.msg;

/**
 * @author jie-r
 * @since 2022/8/15
 */
public interface RemoteMsgHandler {

    /**
     * Handle req msg, one meg type one MsgHandler
     *
     * @param context msg context, include channel or other param
     * @return resp remote msg
     */
    RemoteMsg<?> handleMsg(RemoteMsgContext context);

}
