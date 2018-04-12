package client;

import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class FrameInfo  extends PerformanceStatistics implements FrameAccessor{
	private String stream;
	private LinkedList<StreamServiceClient> clients;
	private HashMap<Integer, StreamFrame> frames;
	private StreamInfo streamInfo;
	private LinkedList<Thread> threads;

	private AtomicInteger readBlocksUntil, current;

	public FrameInfo(String stream){
		super();
    streamInfo = null;
		threads = new LinkedList<>();
		readBlocksUntil = new AtomicInteger(0);
		current = new AtomicInteger(0);
		clients = new LinkedList<>();
		frames = new HashMap<>();
		this.stream = stream;
		startTime();

	}

	public void addClient(StreamServiceClient c) {
		clients.add(c);
		Thread t = new Thread() {
			@Override
			public void run() {
				threadRunner(c, stream);
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
		readBlocksUntil.set(streamInfo.getHeightInBlocks()*streamInfo.getWidthInBlocks());
		return frames.get(frame);

	}


	/* Statistics */
	@Override
	public PerformanceStatistics getPerformanceStatistics() {
		stopTime();
		threads.forEach(Thread::interrupt);
		return this;
	}


	private synchronized boolean setInfo(StreamServiceClient c) {
		while (streamInfo == null) {
			try {
				for (StreamInfo s : c.listStreams()) {
					if (s.getName().equals(stream)) {
						streamInfo = s;
					}
				}
				setDim(streamInfo.getWidthInBlocks(), streamInfo.getHeightInBlocks());
				//Get the first 100 Frames
				readBlocksUntil.set(streamInfo.getWidthInBlocks() * streamInfo.getHeightInBlocks() * 100);
			} catch (IOException e) {
				System.err.println("Cant get streaminfo from "+c.getHost()+"! Cause:"+e.getLocalizedMessage());
				return false;
			}
		}
		return true;
	}

	public void threadRunner(final StreamServiceClient c, final String stream) {
		while (streamInfo == null)
			if (!setInfo(c)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}

		while(!Thread.interrupted()) {
			int blockIndex = current.getAndIncrement();
			downloadBlock(c, blockIndex);
		}
	}

	private void downloadBlock(StreamServiceClient c, int blockIndex) {
		int frameIndex = (int) Math.floor((double)blockIndex / (double)(streamInfo.getHeightInBlocks() *streamInfo.getWidthInBlocks()));
		if (frameIndex > streamInfo.getLengthInFrames()) {
			System.err.println("Got em all!");
			stopTime();
			return;
		}

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