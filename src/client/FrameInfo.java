package client;

import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.DefaultStreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceDiscovery;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.toIntExact;

public class FrameInfo  extends PerformanceStatistics implements FrameAccessor{
	private String stream;
	private LinkedList<StreamServiceClient> clients;
	private HashMap<Integer, StreamFrame> frames;
	private StreamInfo streamInfo;
	private LinkedList<Thread> threads = new LinkedList<>();
	private AtomicInteger readBlocksUntil = new AtomicInteger(0);
	private AtomicInteger current = new AtomicInteger(0);
	private AtomicLong[] highestLatency = new AtomicLong[4];

	public FrameInfo(String stream){
		super();
    streamInfo = null;
		startTime();
		System.err.println("Creating FrameInfo for stream: "+stream);
		this.stream = stream;
		clients = new LinkedList<>();
		frames = new HashMap<>();
		for (int i = 0; i < 4; i++) {
			highestLatency[i] = new AtomicLong(0);
		}

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

	/* This method is blocking */
	@Override
	public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
		int d = -1;
		StreamFrame f = null;
		while (f == null) {
			f = frames.get(frame);
		}
		while(d != 0) {
			d = f.blocksLeftToDownload();
			if (d > 5) {
				try {Thread.sleep(d*2);} catch (InterruptedException e) {}
			}
		}
		return f;

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
				readBlocksUntil.set(streamInfo.getWidthInBlocks() * streamInfo.getHeightInBlocks() * 100);
			} catch (IOException e) {
				System.err.println("Cant get streaminfo from "+c.getHost()+"! Cause:"+e.getLocalizedMessage());
				return false;
			}
		}
		return true;
	}

	public void threadRunner(StreamServiceClient c, final String stream) {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		int theHost = 0;
		for (String s : hosts) {
			if (s.equals(c.getHost()))
				break;
			theHost++;
		}
		while (streamInfo == null)
			if (!setInfo(c)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}

		int numberOfDownloadsForThread = 0;
		while(!Thread.interrupted()) {
			if (numberOfDownloadsForThread > 0 && numberOfDownloadsForThread % 100 == 0) {
				try {
					//System.err.println(c.getHost()+" "+highestLatency[theHost].get());
					c = DefaultStreamServiceClient.bind(c.getHost(), toIntExact(highestLatency[theHost].get()), Client.getInstance().username);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			int blockIndex = current.getAndIncrement();
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

			int framestart = frameIndex > 0 ? (frameIndex*streamInfo.getWidthInBlocks()*streamInfo.getHeightInBlocks()) : 0;
			int x = (blockIndex - framestart) / streamInfo.getHeightInBlocks();
			int y = (blockIndex - framestart) % streamInfo.getHeightInBlocks();
			int tries = 0;
			while (!Thread.interrupted()) {
				tries++;
				try {
				//	System.err.println("frameIndex: " + frameIndex + ", x: "+x + " , y: "+y +", framestart: " +framestart +", block:"+blockIndex + " ["+streamInfo.getWidthInBlocks()+"x"+ streamInfo.getHeightInBlocks()+"]");
					long time = System.currentTimeMillis();
					if (f.downloadBlock(c, x, y)==0) {
						addFrame();
					}
					time = System.currentTimeMillis() - time;
					addPacketLatency(c.getHost(), time);
					if (highestLatency[theHost].get() < time) {
						highestLatency[theHost].set(time);
					}
					break;
				} catch (SocketTimeoutException e) {
					addTimeOut(c.getHost());
					if (tries > 1) {
						System.err.println("Tried get block twice ("+x+","+y+") from frame "+frameIndex+", taking next");
						f.disregardBlock();
						break;
					}
				} catch (IOException e) {
					System.out.println("some IO error.");
						f.disregardBlock();
						break;
				}
			}
			numberOfDownloadsForThread++;
		}
	}
}