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
  private HashMap<Integer, StreamFrame> frames;
  private AtomicInteger currentBlock;
  private Semaphore available = null;
  private StreamInfo streamInfo;
  private LinkedList<Thread> threads = new LinkedList<>();

  public FrameInfo(String stream){
    super();
    startTime();
    System.out.println("Creating FrameInfo for stream: "+stream);
    this.stream = stream;
    clients = new LinkedList<>();
    frames = new HashMap<>();
    currentBlock = new AtomicInteger(1);

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
    threads.add(t);
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
    available.release(streamInfo.getHeightInBlocks()*streamInfo.getWidthInBlocks());
    return frames.get(frame);

  }


  /* Statistics */
  @Override
  public PerformanceStatistics getPerformanceStatistics() {
    stopTime();
    for (Thread t : threads) {
      t.interrupt();
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return this;
  }


  public void threadruns(final StreamServiceClient c, final String stream) {
    synchronized (this) {
      while (streamInfo == null) {
        try {
          for (StreamInfo s : c.listStreams()) {
            if (s.getName().equals(stream)) {
              streamInfo = s;
            }
          }
        } catch (IOException e) {}
        available = new Semaphore(streamInfo.getWidthInBlocks() * streamInfo.getHeightInBlocks() * 100, true);
      }
    }

    while(!Thread.interrupted()) {
      available.acquireUninterruptibly();
      int blockIndex = currentBlock.incrementAndGet();
      int frameIndex = (int) Math.floor((double)blockIndex / (double)streamInfo.getHeightInBlocks() / (double)streamInfo.getWidthInBlocks());
      StreamFrame f = null;
      synchronized (this) {
        if (!frames.containsKey(frameIndex)){
          f = new StreamFrame(frameIndex, stream, streamInfo.getWidthInBlocks(), streamInfo.getHeightInBlocks());
          frames.put(frameIndex, f);
        } else {
          f = frames.get(frameIndex);
        }
      }

      while (!Thread.interrupted()) {
        try {
          int framestart = frameIndex > 0 ? blockIndex / frameIndex : 0;
          int x = streamInfo.getHeightInBlocks() % (blockIndex - framestart);
          int y = (blockIndex - framestart) / streamInfo.getWidthInBlocks();


          long time = System.currentTimeMillis();
          f.downloadBlock(c, x, y);
          time = System.currentTimeMillis() - time;
          addPacketLatency(c.getHost(), time);
          break;
        } catch (SocketTimeoutException e) {
          addTimeOut(c.getHost());
        } catch (IOException e) {
          System.out.println("some IO error.");
        }
      }
    }
  }
}