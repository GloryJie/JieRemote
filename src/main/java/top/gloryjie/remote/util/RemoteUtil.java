package top.gloryjie.remote.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author jie-r
 * @since 2022/8/22
 */
public class RemoteUtil {

    public static SocketAddress string2SocketAddress(final String addr) {
        var split = addr.lastIndexOf(":");
        var host = addr.substring(0, split);
        var port = addr.substring(split + 1);
        return new InetSocketAddress(host, Integer.parseInt(port));
    }


    public static String exceptionSimpleDesc(final Throwable e) {
        StringBuilder sb = new StringBuilder();
        if (e != null) {
            sb.append(e);
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                sb.append(", ");
                sb.append(element.toString());
            }
        }
        return sb.toString();
    }
}
