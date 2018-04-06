package client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.io.IOException;
import java.net.SocketTimeoutException;


public class StreamFrame implements client.FrameAccessor.Frame {

    private Block[][] blocks;

    StreamFrame(String stream, StreamServiceClient c, int frameId) {

        try {
            StreamInfo[] info = c.listStreams();
            for( StreamInfo s : info){
                if(s.getName().equals(stream)){


                    this.blocks = new Block[s.getWidthInBlocks()][s.getHeightInBlocks()];

                    for(int i = 0; i < s.getWidthInBlocks(); i++){
                        for(int j = 0; j < s.getHeightInBlocks(); j++){

                            System.out.println(stream+" "+frameId +" "+i+" "+j);
                            try {
                                this.blocks[i][j] = c.getBlock(stream, frameId, i, j);
                            } catch (SocketTimeoutException e){
                                System.out.println("Socket timed out");
                            }

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.getStackTrace();

        }

    }

    @Override
    public Block getBlock(int blockX, int blockY) {
        return blocks[blockX][blockY];
    }
}
