package client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class FrameInfo  extends PerformanceStatistics implements FrameAccessor, FrameAccessor.Frame {


  private StreamServiceClient clients[];
  private String stream;

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

}