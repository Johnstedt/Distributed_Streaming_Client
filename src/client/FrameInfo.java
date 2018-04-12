package client;

import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *	The FrameAccessor, it is implemented to take care of one Stream.
 */
public class FrameInfo  extends PerformanceStatistics implements FrameAccessor{
	private String stream;
	private HashMap<Integer, StreamFrame> frames;
	private StreamInfo streamInfo;
	private LinkedBlockingQueue<Thread> threads;
	private AtomicInteger downloadBlocksUntil, currentBlockToDownload;
	private int frameWidth, frameHeight;

	/**
	 * Will set the stream and other variables.
	 * @param stream	The stream that is subject for downloads.
	 */
	public FrameInfo(String stream){
		super();
    streamInfo = null;
		threads = new LinkedBlockingQueue<>();
		downloadBlocksUntil = new AtomicInteger(0);
		currentBlockToDownload = new AtomicInteger(0);
		frames = new HashMap<>();
		this.stream = stream;
	}

	/**
	 * Adds a new StreamServiceClient to the stream, each client gets a new Thread.
	 * @param c	The given StreamServiceClient
	 */
	public void addClient(StreamServiceClient c) {
		Thread t = new Thread(() -> threadRunner(c));
		t.start();
		threads.add(t);
	}


	@Override
	public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {
		return streamInfo;
	}

	@Override
	public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
		downloadBlocksUntil.set(streamInfo.getHeightInBlocks()*streamInfo.getWidthInBlocks()*(100+frame));
		return frames.get(frame);

	}


	/**
	 * Will assume when statistics requested that all downloads are done.
	 * @return	The statistics for this Stream.
	 */
	@Override
	public PerformanceStatistics getPerformanceStatistics() {
		stopTime();
		threads.forEach(Thread::interrupt);
		return this;
	}

	/**
	 * Will run until all threads are added.
	 */
	private void threadRunner(final StreamServiceClient c) {
		while (streamInfo == null)
			if (!setInfo(c)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored){}
			}

		while (!Thread.interrupted()) {
			addBlockToFrame(c, currentBlockToDownload.getAndIncrement());
		}
	}


	/**
	 * This will happen before any download to get and set meta-data.
	 * @param c		The given StreamServiceClient to download from.
	 * @return		If successfully downloaded the data.
	 */
	private synchronized boolean setInfo(StreamServiceClient c) {
		while (streamInfo == null) {
			try {
				for (StreamInfo s : c.listStreams()) {
					if (s.getName().equals(stream)) {
						streamInfo = s;
					}
				}
				setDim(streamInfo.getWidthInBlocks(), streamInfo.getHeightInBlocks());
				frameHeight = streamInfo.getHeightInBlocks();
				frameWidth = streamInfo.getWidthInBlocks();
				//Initialize to get the first 100 Frames
				downloadBlocksUntil.set(streamInfo.getWidthInBlocks() * streamInfo.getHeightInBlocks() * 100);
			} catch (IOException e) {
				System.err.println("Cant get streaminfo from "+c.getHost()+"! Cause:"+e.getLocalizedMessage());
				return false;
			}
		}
		return true;
	}

	private void addBlockToFrame(StreamServiceClient c, int blockIndex) {
		int frameIndex = (int) Math.floor((double)blockIndex / (double)(frameWidth * frameHeight));

		/* Finished all blocks */
		if (frameIndex > streamInfo.getLengthInFrames()) {
			getPerformanceStatistics();
			return;
		}

		/* Synchroized so no two threads try to add same frame */
		StreamFrame f;
		synchronized (this) {
			if (!frames.containsKey(frameIndex)){
				f = new StreamFrame(frameIndex, stream, frameWidth, frameHeight);
				frames.put(frameIndex, f);
			} else {
				f = frames.get(frameIndex);
			}
		}

		/* Get the block and add it to the frame. */
		int frameStart = frameIndex > 0 ? (frameIndex*frameWidth*frameHeight) : 0;
		int x = (blockIndex - frameStart) / frameHeight;
		int y = (blockIndex - frameStart) % frameHeight;
		getBlock(c, f, x, y);
	}

	/**
	 * This will get the given block from the StreamClient and update statistic variables.
	 * @param c						The StreamServiceClient that will download the block.
	 * @param f						The StramFrame that will download the
	 * @param x						The block index x-axis
	 * @param y						The block index y-axis
	 */
	private void getBlock(StreamServiceClient c, StreamFrame f, int x, int y) {
		long time = System.currentTimeMillis();

		/* Blocking in the manner that it will try until it get the frame */
		while (!Thread.interrupted()) {
			try {
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