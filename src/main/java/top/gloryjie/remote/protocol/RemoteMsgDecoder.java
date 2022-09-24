package top.gloryjie.remote.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import lombok.extern.slf4j.Slf4j;
import top.gloryjie.remote.protocol.msg.RemoteMsg;

import java.util.List;

import static top.gloryjie.remote.protocol.msg.RemoteMsg.*;

/**
 * decoder is stateful，can't be sharable
 *
 * @author jie-r
 * @since 2022/8/23
 */
@Slf4j
public class RemoteMsgDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        int readableBytes = in.readableBytes();

        // readableBytes need 6bytes to get magic code and totalLen
        if (readableBytes >= JIE_MAGIC_CODE_BYTES + DESC_TOTAL_LEN_BYTES) {
            int totalLen = in.getIntLE(in.readerIndex() + JIE_MAGIC_CODE_BYTES);
            if (readableBytes < totalLen + JIE_MAGIC_CODE_BYTES) {
                // data not enough to split one data frame
                return;
            }

            // start to decode
            short magicCode = in.getShortLE(in.readerIndex());
            if (magicCode != JIE_MAGIC_CODE) {
                // not valid magic code, could break this channel
                throw new DecoderException("invalid magic code");
            } else {
                RemoteMsg<?> protocol = new RemoteMsg<>();

                // slice a complete data frame
                ByteBuf frameBuf = in.retainedSlice(in.readerIndex(), totalLen + JIE_MAGIC_CODE_BYTES);
                // set new reader index
                in.readerIndex(in.readerIndex() + totalLen + JIE_MAGIC_CODE_BYTES);

                // skip magic code
                frameBuf.readShortLE();

                totalLen = frameBuf.readIntLE();
                int headerLen = frameBuf.readShortLE();

                // header param
                protocol.setVersion(frameBuf.readByte());
                protocol.setSerializeType(frameBuf.readByte());
                protocol.setCompressType(frameBuf.readByte());
                protocol.setMsgType(frameBuf.readShortLE());
                protocol.setMsgId(frameBuf.readIntLE());
                protocol.setFlag(frameBuf.readByte());

                // hava header ext data
                if (headerLen > DESC_HEADER_LEN_BYTES + HEADER_FIX_BYTES) {
                    byte[] headerExt = new byte[headerLen - DESC_HEADER_LEN_BYTES - HEADER_FIX_BYTES];
                    frameBuf.readBytes(headerExt);
                    protocol.setHeaderExtBytes(headerExt);
                }
                // body data
                int bodyLen = totalLen - DESC_TOTAL_LEN_BYTES - headerLen;
                if (bodyLen > 0) {
                    byte[] bodyBytes = new byte[bodyLen];
                    frameBuf.readBytes(bodyBytes);
                    protocol.setBodyBytes(bodyBytes);
                }
                out.add(protocol);
            }

        }
    }
}
