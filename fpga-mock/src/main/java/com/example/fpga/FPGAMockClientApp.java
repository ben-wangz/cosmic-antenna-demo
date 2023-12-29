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

  private static Long counter = 0L;

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
        Optional.ofNullable(System.getenv("DATA_CHUNK_SIZE"))
            .map(Integer::parseInt)
            .orElse(channelSize * timeSampleSize);
    int antennaStartIndex =
        Optional.ofNullable(System.getenv("ANTENNA_START_INDEX")).map(Integer::parseInt).orElse(0);
    try (FPGAMockClient client = FPGAMockClient.builder().port(port).build()) {
      ChannelFuture channelFuture = client.startup(host);
      LOGGER.info(
          "A new FPGA Mock Client is created, [{}:{}, iterator:{}, interval:{}, package size:{}]",
          host,
          port,
          count,
          interval,
          dataChunkSize * 2 + 8);
      for (long index = count; index != 0; index--) {
        if (channelFuture.isSuccess()) {
          channelFuture
              .channel()
              .writeAndFlush(
                  Unpooled.wrappedBuffer(randomRecord(antennaStartIndex, dataChunkSize)));
        }
        Thread.sleep(interval);
      }
    }
  }

  private static byte[] randomRecord(int antennaStartIndex, int dataChunkSize) {
    Random random = new Random();
    byte[] resultArray = new byte[8 + dataChunkSize * 2];
    ByteBuffer byteBuffer = ByteBuffer.wrap(resultArray);

    byte antennaId = (byte) random.nextInt(antennaStartIndex + 8);
    byteBuffer.put(ByteBuffer.allocate(1).put(antennaId).array());

    LOGGER.info(
        "Sent antennaId:{}, counter:{}, size of array:{} ",
        antennaId & 0xFF,
        counter,
        dataChunkSize);

    byteBuffer.put(longToBytes(counter++));

    byte[] data = new byte[dataChunkSize * 2];
    random.nextBytes(data);
    byteBuffer.put(data);

    return resultArray;
  }

  public static byte[] longToBytes(long l) {
    byte[] result = new byte[7];
    for (int i = 6; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= Byte.SIZE;
    }
    return result;
  }
}
