package client;

import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;
import sun.awt.Mutex;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;

public class FrameInfo  extends PerformanceStatistics implements FrameAccessor{
	private String stream;
	private LinkedList<StreamServiceClient> clients;
	private HashMap<Integer, StreamFrame> frames;
	private StreamInfo streamInfo;
	private LinkedList<Thread> threads = new LinkedList<>();


	Mutex get = new Mutex();
	private int readuntil = 0;
	private int current = 0;

	public FrameInfo(String stream){
		super();
    streamInfo = null;
		startTime();
		System.err.println("Creating FrameInfo for stream: "+stream);
		this.stream = stream;
		clients = new LinkedList<>();
		frames = new HashMap<>();

	}

	public void addClient(StreamServiceClient c) {
		clients.add(c);
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
		return streamInfo;
	}

	@Override
	public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
		//TODO IF not exists = block.
		get.lock();
		readuntil += streamInfo.getHeightInBlocks()*streamInfo.getWidthInBlocks();
		get.unlock();
		return frames.get(frame);

	}


	/* Statistics */
	@Override
	public PerformanceStatistics getPerformanceStatistics() {
		stopTime();
		threads.forEach(Thread::interrupt);
		return this;
	}


	private synchronized boolean setinfo(StreamServiceClient c) {
		while (streamInfo == null) {
			try {
				for (StreamInfo s : c.listStreams()) {
					if (s.getName().equals(stream)) {
						streamInfo = s;
					}
				}
				setDim(streamInfo.getWidthInBlocks(), streamInfo.getHeightInBlocks());
				get.lock();
				current = 0;
				readuntil = streamInfo.getWidthInBlocks() * streamInfo.getHeightInBlocks() * 100;
				get.unlock();
			} catch (IOException e) {
				System.err.println("Cant get streaminfo from "+c.getHost()+"! Cause:"+e.getLocalizedMessage());
				return false;
			}
		}
		return true;
	}
	public void threadruns(final StreamServiceClient c, final String stream) {
		while (streamInfo == null)
			if (!setinfo(c)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}

		while(!Thread.interrupted()) {
			get.lock();
			int blockIndex = current++;
			get.unlock();
			int frameIndex = (int) Math.floor((double)blockIndex / (double)(streamInfo.getHeightInBlocks() *streamInfo.getWidthInBlocks()));
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

					int framestart = frameIndex > 0 ? (frameIndex*streamInfo.getWidthInBlocks()*streamInfo.getHeightInBlocks()) : 0;
					int x = (blockIndex - framestart) / streamInfo.getHeightInBlocks();
					int y = (blockIndex - framestart) % streamInfo.getHeightInBlocks();
				//	System.err.println("frameIndex: " + frameIndex + ", x: "+x + " , y: "+y +", framestart: " +framestart +", block:"+blockIndex + " ["+streamInfo.getWidthInBlocks()+"x"+ streamInfo.getHeightInBlocks()+"]");
					long time = System.currentTimeMillis();
					if (f.downloadBlock(c, x, y)==0) {
						addFrame();
					}
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