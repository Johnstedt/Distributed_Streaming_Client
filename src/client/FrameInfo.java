package client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class FrameInfo implements FrameAccessor, FrameAccessor.Frame, FrameAccessor.PerformanceStatistics{



  /* Frame */
  @Override
  public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {

    return null;
  }

  @Override
  public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
    return null;
  }

  /* Statistics */
  @Override
  public PerformanceStatistics getPerformanceStatistics() {
    return null;
  }


  @Override
  public Block getBlock(int blockX, int blockY) throws IOException, SocketTimeoutException {
    return null;
  }

  @Override
  public double getPacketDropRate(String host) {
    return 0;
  }

  @Override
  public double getPacketLatency(String host) {
    return 0;
  }

  @Override
  public double getFrameThroughput() {
    return 0;
  }

  @Override
  public double getBandwidthUtilization() {
    return 0;
  }
}
