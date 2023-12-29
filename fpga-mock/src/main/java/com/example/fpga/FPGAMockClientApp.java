package com.example.fpga;

import com.example.fpga.client.FPGAMockClient;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FPGAMockClientApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(FPGAMockClientApp.class);

  public static void main(String[] args) throws Exception {
    LOGGER.info("Creating a new FPGA UDP Client");
    String host = Optional.ofNullable(System.getenv("FPGA_CLIENT_HOST")).orElse("10.11.32.44");
    int port =
        Optional.ofNullable(System.getenv("FPGA_CLIENT_PORT")).map(Integer::parseInt).orElse(18888);
    int count =
        Optional.ofNullable(System.getenv("RECORD_COUNT")).map(Integer::parseInt).orElse(-1);
    int interval =
        Optional.ofNullable(System.getenv("RECORD_INTERVAL")).map(Integer::parseInt).orElse(1000);
    int channelSize =
            Optional.ofNullable(System.getenv("CHANNEL_SIZE")).map(Integer::parseInt).orElse(10);
    int timeSampleSize =
            Optional.ofNullable(System.getenv("TIME_SAMPLE_SIZE")).map(Integer::parseInt).orElse(16);
    int dataChunkSize =
        Optional.ofNullable(System.getenv("DATA_CHUNK_SIZE")).map(Integer::parseInt)
                .orElse(channelSize * timeSampleSize);
    try (FPGAMockClient client = FPGAMockClient.builder().port(port).build()) {
      ChannelFuture channelFuture = client.startup(host);
      LOGGER.info(
          "A new FPGA Mock Client is created, [{}:{}, iterator:{}, interval:{}, package size:{}]",
          host,
          port,
          count,
          interval, dataChunkSize * 2 + 8);
      for (long index = count; index != 0; index--) {
        if (channelFuture.isSuccess()) {
          channelFuture
              .channel()
              .writeAndFlush(Unpooled.wrappedBuffer(randomRecord(dataChunkSize)));
        }
        Thread.sleep(interval);
      }
    }
  }

  private static byte[] randomRecord(int dataChunkSize) {
    Random random = new Random();
    byte[] resultArray = new byte[8 + dataChunkSize * 2];
    ByteBuffer byteBuffer = ByteBuffer.wrap(resultArray);

    byte antennaId = (byte) random.nextInt(224);
    byteBuffer.put(ByteBuffer.allocate(1).put(antennaId).array());

    byte[] counter = new byte[7];
    random.nextBytes(counter);
    byteBuffer.put(counter);

    byte[] paddedCounter = new byte[8];
    System.arraycopy(counter, 0, paddedCounter, 1, counter.length);

    LOGGER.info(
        "Sent antennaId:{}, counter:{}, size of array: {} ",
        antennaId,
        ByteBuffer.wrap(paddedCounter).getLong(),
        dataChunkSize);

    byte[] data = new byte[dataChunkSize * 2];
    random.nextBytes(data);
    byteBuffer.put(data);

    return resultArray;
  }
}
