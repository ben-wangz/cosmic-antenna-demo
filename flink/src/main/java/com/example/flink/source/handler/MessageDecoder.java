package com.example.flink.source.handler;

import com.example.flink.data.SampleData;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBufUtil;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramPacket;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * {@link MessageDecoder} is the Message decoder from byte to string using {@link MessageToMessageDecoder}.
 *
 * @author Sameer Narkhede See <a href="https://narkhedesam.com">https://narkhedesam.com</a>
 * @since Sept 2020
 */
public class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {

//        InetSocketAddress sender = packet.sender();

        ByteBuf in = packet.content();
        int readableBytes = in.readableBytes();
        if (readableBytes <= 0) {
            return;
        }

        //get data from datagram package
        Object deserialized = SerializationUtils.deserialize(ByteBufUtil.getBytes(in));

        if (deserialized instanceof SampleData) {
            ((SampleData) deserialized).setSender(packet.sender());
            out.add(deserialized);
        }
    }

}
