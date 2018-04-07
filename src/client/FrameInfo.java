package client;

import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameInfo  extends PerformanceStatistics implements FrameAccessor{
  private String stream;
  private LinkedList<StreamServiceClient> clients;
  private HashMap<Integer, Frame> frames;
  private AtomicInteger currentFrame;
  private final Semaphore available = new Semaphore(100, true);

  public FrameInfo(String stream){
    super();
    startTime();
    System.out.println("Creating FrameInfo for stream: "+stream);
    this.stream = stream;
    clients = new LinkedList<>();
    frames = new HashMap<>();
    currentFrame = new AtomicInteger(1);

  }

  public void addClient(StreamServiceClient c) {
    clients.add(c);
    System.out.println("added thread for "+stream);
    Thread t = new Thread() {
      @Override
      public void run() {
        threadruns(c, stream);
      }
    };
    t.start();
    //TODO: Make a run for it ^^ aka hämta frames.

  }


  /* Frame */
  @Override
  public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {
    return null;
  }

  @Override
  public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
    //TODO IF not exists = block.
    available.release();
    return frames.get(frame);

  }


  /* Statistics */
  @Override
  public PerformanceStatistics getPerformanceStatistics() {
    stopTime();
    return this;
  }


  public void threadruns(StreamServiceClient c, String stream) {
    System.out.println("new thread in stream"+stream);
    while(true) {
      available.acquireUninterruptibly();
      int frameIndex = currentFrame.incrementAndGet();
      Frame f = null;

      try {
        long time = System.currentTimeMillis();
        f = new StreamFrame(stream, c, frameIndex);
        System.out.println("put frame");
        time = System.currentTimeMillis() - time;
        addPacketLatency(c.getHost(), time);
        frames.put(frameIndex, f);
        addFrame();

      } catch (SocketTimeoutException e) {
        addTimeOut(c.getHost());
      } catch (IOException e) {
        System.out.println("some IO error.");
      }


    }
  }
}