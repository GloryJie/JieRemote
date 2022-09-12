package top.gloryjie.remote.protocol;

import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.gloryjie.remote.protocol.msg.RemoteMsg;

import java.util.ArrayList;

/**
 * @author jie-r
 * @since 2022/8/23
 */
public class RemoteMsgCodecTest {

    @Test
    public void onewayMsgTest() throws Exception {
        var decoder = new RemoteMsgDecoder();
        var encoder = new RemoteMsgEncoder();

        var remoteMsg = getDefaultMsg();
        remoteMsg.markOnewayFlag();

        var buffer = PooledByteBufAllocator.DEFAULT.buffer();

        encoder.encode(null, remoteMsg, buffer);

        var list = new ArrayList<>();
        decoder.decode(null, buffer, list);

        var decodeMsg = (RemoteMsg<?>) list.get(0);
        Assertions.assertFalse(decodeMsg.isResponse());
        Assertions.assertFalse(decodeMsg.isRequest());
        Assertions.assertTrue(decodeMsg.isOneway());
    }

    @Test
    public void respMsgTest() throws Exception {
        var decoder = new RemoteMsgDecoder();
        var encoder = new RemoteMsgEncoder();

        var remoteMsg = getDefaultMsg();
        remoteMsg.markRespFlag();

        var buffer = PooledByteBufAllocator.DEFAULT.buffer();

        encoder.encode(null, remoteMsg, buffer);

        var list = new ArrayList<>();
        decoder.decode(null, buffer, list);

        var decodeMsg = (RemoteMsg<?>) list.get(0);
        Assertions.assertTrue(decodeMsg.isResponse());
        Assertions.assertFalse(decodeMsg.isRequest());
        Assertions.assertFalse(decodeMsg.isOneway());
    }

    @Test
    public void reqMsgTest() throws Exception {
        var decoder = new RemoteMsgDecoder();
        var encoder = new RemoteMsgEncoder();
        var remoteMsg = getDefaultMsg();
        remoteMsg.markReqFlag();

        var buffer = PooledByteBufAllocator.DEFAULT.buffer();

        encoder.encode(null, remoteMsg, buffer);

        var list = new ArrayList<>();
        decoder.decode(null, buffer, list);

        var decodeMsg = (RemoteMsg<?>) list.get(0);
        Assertions.assertFalse(decodeMsg.isResponse());
        Assertions.assertTrue(decodeMsg.isRequest());
        Assertions.assertFalse(decodeMsg.isOneway());
    }

    @Test
    public void codecTest() throws Exception {
        var decoder = new RemoteMsgDecoder();
        var encoder = new RemoteMsgEncoder();

        var remoteMsg = getDefaultMsg();

        var buffer = PooledByteBufAllocator.DEFAULT.buffer();

        encoder.encode(null, remoteMsg, buffer);

        var list = new ArrayList<>();
        decoder.decode(null, buffer, list);

        var decodeMsg = (RemoteMsg<?>) list.get(0);
        Assertions.assertEquals(remoteMsg.getSerializeType(), decodeMsg.getSerializeType());
        Assertions.assertEquals(remoteMsg.getCompressType(), decodeMsg.getCompressType());
        Assertions.assertEquals(remoteMsg.getMsgType(), decodeMsg.getMsgType());
        Assertions.assertEquals(remoteMsg.getMsgId(), decodeMsg.getMsgId());
        Assertions.assertEquals(remoteMsg.getFlag(), decodeMsg.getFlag());
    }

    private RemoteMsg<String> getDefaultMsg() {
        RemoteMsg<String> remoteMsg = (RemoteMsg<String>) RemoteMsg.createRequest();
        remoteMsg.setSerializeType(1);
        remoteMsg.setCompressType(0);
        remoteMsg.setMsgType(1);
        remoteMsg.setMsgId(1);

        remoteMsg.setHeaderExtBytes(new byte[]{1, 2, 3});
        remoteMsg.setBodyBytes(new byte[]{1, 2, 3});
        return remoteMsg;
    }


}
