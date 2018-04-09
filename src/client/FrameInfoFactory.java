package client;

import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.util.HashMap;
import java.util.LinkedList;

public class FrameInfoFactory implements FrameAccessor.Factory {
	public class FrameAccessorClient {
		FrameInfo frameAccessor;
		LinkedList<StreamServiceClient> streamServiceClients;

		FrameAccessorClient(FrameInfo frameAccessor) {
			this.frameAccessor = frameAccessor;
			this.streamServiceClients = new LinkedList<>();
		}

		FrameAccessorClient(FrameInfo frameAccessor, StreamServiceClient scc) {
			this(frameAccessor);
		}

		void addClient(StreamServiceClient ssc) {
			streamServiceClients.add(ssc);
			frameAccessor.addClient(ssc);
		}
	}

	private static FrameInfoFactory instance = null;
	private HashMap<String, FrameAccessorClient> sFA = new HashMap<>();
	protected FrameInfoFactory() {

	}
	public static FrameInfoFactory getInstance() {
		if (instance == null) {
			instance = new FrameInfoFactory();
		}
		return instance;
	}

	@Override
	public FrameAccessor getFrameAccessor(StreamServiceClient client, String stream) {
		if (!sFA.containsKey(stream)) {
			FrameInfo fi = new FrameInfo(stream);
			FrameAccessorClient fac = new FrameAccessorClient(fi, client);
			sFA.put(stream, fac);
		}
		sFA.get(stream).addClient(client);
		return sFA.get(stream).frameAccessor;
	}

	@Override
	public FrameAccessor getFrameAccessor(StreamServiceClient[] clients, String stream) {
		FrameAccessorClient fac = sFA.get(stream);
		for (StreamServiceClient c : clients) {
			getFrameAccessor(c, stream);
		}
		return fac.frameAccessor;
	}
}