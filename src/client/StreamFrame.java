package client;

import ki.types.ds.Block;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class StreamFrame implements client.FrameAccessor.Frame {
	private final String stream;
	private int id;
	private Block[][] blocks;
	private AtomicInteger blocksLeftToDownload;
	private AtomicInteger disregarded;

	StreamFrame(int id, String stream, int x, int y){
		this.id = id;
		this.blocks = new Block[x][y];
		this.stream = stream;
		this.blocksLeftToDownload = new AtomicInteger(x*y);
		this.disregarded = new AtomicInteger(0);
	}

	@Override
	public Block getBlock(int blockX, int blockY) {
		return this.blocks[blockX][blockY];
	}

	int downloadBlock(StreamServiceClient c, int x, int y) throws IOException {
		while(true) {
			try {
				this.blocks[x][y] = c.getBlock(stream, id, x, y);
				return blocksLeftToDownload.decrementAndGet();
			} catch (ClassCastException e) {
				System.err.println("Cannot cast nothing that was not never gotten");
			}
		}
	}

	void disregardBlock() {
		blocksLeftToDownload.decrementAndGet();
		disregarded.decrementAndGet();
	}

	int blocksLeftToDownload() {
		return blocksLeftToDownload.get();
	}
}