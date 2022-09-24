package top.gloryjie.remote.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.gloryjie.remote.protocol.msg.RemoteMsg;

import static top.gloryjie.remote.protocol.msg.RemoteMsg.*;

/**
 * @author jie-r
 * @since 2022/8/23
 */
@ChannelHandler.Sharable
public class RemoteMsgEncoder extends MessageToByteEncoder<RemoteMsg<?>> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemoteMsg<?> msg, ByteBuf byteBuf) throws Exception {
        // 2 byte magic code
        byteBuf.writeShortLE(JIE_MAGIC_CODE);
        int lenIndex = byteBuf.writerIndex();
        // skip totalLen & headerLen
        byteBuf.writeIntLE(0);
        byteBuf.writeShortLE(0);

        // header param
        byteBuf.writeByte(msg.getVersion());
        byteBuf.writeByte(msg.getSerializeType());
        byteBuf.writeByte(msg.getCompressType());
        byteBuf.writeShortLE(msg.getMsgType());
        byteBuf.writeIntLE(msg.getMsgId());
        byteBuf.writeByte(msg.getFlag());

        // header binary
        int headerLen = DESC_HEADER_LEN_BYTES + HEADER_FIX_BYTES;
        if (msg.getHeaderExtBytes() != null && msg.getHeaderExtBytes().length > 0) {
            byteBuf.writeBytes(msg.getHeaderExtBytes());
            headerLen += msg.getHeaderExtBytes().length;
        }
        // header binary
        int bodyLen = 0;
        if (msg.getBodyBytes() != null && msg.getBodyBytes().length > 0) {
            byteBuf.writeBytes(msg.getBodyBytes());
            bodyLen = msg.getBodyBytes().length;
        }
        // 2byte to describe header total length
        byteBuf.setShortLE(lenIndex + DESC_TOTAL_LEN_BYTES, headerLen);

        // 4 byte to describe total length
        byteBuf.setIntLE(lenIndex, DESC_TOTAL_LEN_BYTES + headerLen + bodyLen);
    }
}
