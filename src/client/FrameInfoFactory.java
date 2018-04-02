package client;

import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.util.HashMap;
import java.util.LinkedList;

public class FrameInfoFactory implements FrameAccessor.Factory {
  private HashMap<String, FrameAccessorClient> sFA = new HashMap<>();

  public class FrameAccessorClient {
    FrameAccessor frameAccessor;
    LinkedList<StreamServiceClient> streamServiceClients;

    FrameAccessorClient(FrameAccessor frameAccessor) {
      this.frameAccessor = frameAccessor;
      this.streamServiceClients = new LinkedList<>();
    }

    FrameAccessorClient(FrameAccessor frameAccessor, StreamServiceClient scc) {
      this(frameAccessor);
      addClient(scc);
    }

    void addClient(StreamServiceClient ssc) {
      streamServiceClients.add(ssc);
    }
  }

  @Override
  public FrameAccessor getFrameAccessor(StreamServiceClient client, String stream) {
    if (!sFA.containsKey(stream)) {
      FrameInfo fi = new FrameInfo();
      FrameAccessorClient fac = new FrameAccessorClient(fi, client);
      sFA.put(stream, fac);
    }
    return sFA.get(stream).frameAccessor;
  }

  @Override
  public FrameAccessor getFrameAccessor(StreamServiceClient[] clients, String stream) {
    if (!sFA.containsKey(stream)) {
      FrameInfo fi = new FrameInfo();
      FrameAccessorClient fac = new FrameAccessorClient(fi);
      sFA.put(stream, fac);
    }
    FrameAccessorClient fac = sFA.get(stream);
    for (StreamServiceClient c : clients) {
      fac.addClient(c);
    }
    return fac.frameAccessor;
  }
}
