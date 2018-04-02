package client;

import ki.types.ds.StreamInfo;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class FrameInfo implements FrameAccessor{

  @Override
  public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {

    return null;
  }

  @Override
  public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
    return null;
  }

  @Override
  public PerformanceStatistics getPerformanceStatistics() {
    return null;
  }
}
