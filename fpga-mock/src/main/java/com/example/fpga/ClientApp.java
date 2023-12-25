package com.example.fpga;

import com.example.fpga.client.FPGAMockClient;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientApp.class);

  public static void main(String[] args) throws Exception {
    LOGGER.info("Creating a new FPGA UDP Client");
    String host = Optional.ofNullable(System.getenv("FPGA_CLIENT_HOST")).orElse("127.0.0.1");
    int port =
        Optional.ofNullable(System.getenv("FPGA_CLIENT_PORT")).map(Integer::parseInt).orElse(50330);
    int count =
        Optional.ofNullable(System.getenv("RECORD_COUNT")).map(Integer::parseInt).orElse(-1);
    int interval =
        Optional.ofNullable(System.getenv("RECORD_INTERVAL")).map(Integer::parseInt).orElse(3000);
    int dataChunkSize =
        Optional.ofNullable(System.getenv("DATA_CHUNK_SIZE")).map(Integer::parseInt).orElse(4000);
    try (FPGAMockClient client = FPGAMockClient.builder().port(port).build()) {
      ChannelFuture channelFuture = client.startup(host);
      LOGGER.info(
          "A new FPGA Mock Client is created, [{}:{}, iterator:{}, interval:{}]",
          host,
          port,
          count,
          interval);
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
    System.arraycopy(counter, 0, paddedCounter, 0, counter.length);

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
