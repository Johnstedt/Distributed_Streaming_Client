package client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;


public class StreamFrame implements client.FrameAccessor.Frame {

    private Block[][] blocks;

    StreamFrame(String stream, StreamServiceClient c, int frameId)  throws IOException, SocketTimeoutException {
        StreamInfo[] info = null;
        try {
            info = c.listStreams();
        }catch (java.lang.ClassCastException e) {
            System.err.println("nope 1");
        }
        System.out.println("Yupp 1");
        for( StreamInfo s : info){
            if(s.getName().equals(stream)){
                this.blocks = new Block[s.getWidthInBlocks()][s.getHeightInBlocks()];
                for(int i = 0; i < s.getWidthInBlocks(); i++){
                    for(int j = 0; j < s.getHeightInBlocks(); j++) {
                        try {
                            this.blocks[i][j] = c.getBlock(stream, frameId, i, j);
                        }catch (java.lang.ClassCastException e){
                            System.err.println("nope 2");
                        }
                    }
                }
            }
        }
        System.out.println("SteamFrame done");
    }

    @Override
    public Block getBlock(int blockX, int blockY) {
        return blocks[blockX][blockY];
    }
}
