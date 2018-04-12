package client;

import se.umu.cs._5dv186.a1.client.StreamServiceClient;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * FrameInfoFactory that implement singleton behaivour where a FrameAccessor
 * will be created for each stream. One stream can have several StreamServiceClients.
 */
public class FrameInfoFactory implements FrameAccessor.Factory {

	/**
	 * FrameAccessorClient, class to keep track of created FrameAccessors and Clients.
	 */
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

	/**
	 * Empty constructor.
	 */
	private FrameInfoFactory() {}

	/**
	 * Singleton design factory to only have one instance of the Factory.
	 * @return	The FrameInfoFactory instance.
	 */
	public static FrameInfoFactory getInstance() {
		return (instance == null) ? instance = new FrameInfoFactory(): instance;
	}

	/**
	 * Adds StreamServiceClient To FrameAccessor of given Stream, adds new
	 * FrameAccesor if no FrameAccessor exist for given stream.
	 * @param client	The StreamServiceClient to add to a FrameAccessor
	 * @param stream	The stream the StreamServiceClient will be used for.
	 * @return				The FrameAccesor.
	 */
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

	/**
	 * Adds several StreamServiceClient To FrameAccessor of given Stream, adds new
	 * FrameAccessor if no FrameAccessor exist for given stream.
	 * @param clients	The StreamServiceClients to add to a FrameAccessor
	 * @param stream	The stream the StreamServiceClient will be used for.
	 * @return				The FrameAccesor.
	 */
	@Override
	public FrameAccessor getFrameAccessor(StreamServiceClient[] clients, String stream) {
		FrameAccessorClient fac = sFA.get(stream);
		for (StreamServiceClient c : clients) {
			getFrameAccessor(c, stream);
		}
		return fac.frameAccessor;
	}
}