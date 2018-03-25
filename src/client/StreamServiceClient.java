package client;

import java.io.IOException;
import java.net.SocketTimeoutException;

public interface StreamServiceClient {
    public String getHost ();

    public StreamInfo[] listStreams ()
            throws IOException, SocketTimeoutException;

    public Block getBlock (String stream, int frame, int blockX, int blockY)
            throws IOException, SocketTimeoutException;

}
