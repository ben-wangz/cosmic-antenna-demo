package com.example.fpga;

import com.example.fpga.client.FPGAMockClient;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Random;

public class ClientApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApp.class);

    public static void main(String[] args) {

        try {

            LOGGER.info("Creating a new FPGA UDP Client");

            String host = System.getProperty("host", "127.0.0.1");
            int port = Integer.parseInt(System.getProperty("port", "50283"));
            long iter = Long.parseLong(System.getProperty("iter", "1000")) > 0 ?
                    Long.parseLong(System.getProperty("iter", "1000"))
                    : -1;
            int interval = Integer.parseInt(System.getProperty("interval", "3000"));
            int timeSampleSize = Integer.parseInt(System.getProperty("tSize", "2048"));

            FPGAMockClient client = FPGAMockClient.builder()
                    .port(port)
                    .build();
            ChannelFuture channelFuture = client.startup(host);

            LOGGER.info("A new FPGA Mock Client is created, [{}:{}, iterator:{}, interval:{}]",
                    host, port, iter, interval);

            for (long index = iter; index != 0; index--) {
                if (channelFuture.isSuccess()) {
                    channelFuture.channel()
                            .writeAndFlush(
                                    Unpooled.wrappedBuffer(randomRecord(timeSampleSize))
                            );
                }
                Thread.sleep(interval);
            }
            client.shutdown();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static byte[] randomRecord(int timeSampleSize) {
        Random random = new Random();
        byte[] resultArray = new byte[12 + timeSampleSize * 2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(resultArray);

        short i1 = Double.valueOf(Math.random() * 1000).shortValue();
        ByteBuffer channelId = ByteBuffer.allocate(2);
        channelId.putShort(i1);
        byteBuffer.put(channelId.array());

        short i2 = Double.valueOf(Math.random() * 224).shortValue();
        ByteBuffer antennaId = ByteBuffer.allocate(2);
        antennaId.putShort(i2);
        byteBuffer.put(antennaId.array());

        long longValue = Double.valueOf(Math.random() * Long.MAX_VALUE).longValue();
        LOGGER.info("Sent channelId:{}, antennaId:{}, counter:{} ", i1, i2, longValue);

        ByteBuffer counter = ByteBuffer.allocate(8);
        counter.putLong(longValue);
        byteBuffer.put(counter.array());

        byte[] data = new byte[timeSampleSize * 2];
        byte[] realArray = new byte[timeSampleSize];
        byte[] imaginaryArray = new byte[timeSampleSize];
        random.nextBytes(realArray);
        random.nextBytes(imaginaryArray);


        for (int i = 0; i < timeSampleSize * 2; i++) {
            if (i % 2 == 0) {
                data[i] = realArray[i / 2];
            } else {
                data[i] = imaginaryArray[i / 2];
            }
        }
        byteBuffer.put(data);
        return resultArray;
    }
}
