package client;

import se.umu.cs._5dv186.a1.client.DefaultStreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceDiscovery;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Client {
	static int TIMEOUT = 10;
	/**
	 * Will download one given stream. Stderr will show extra data and Stdout presents data in a multi vector.
	 * @param args	In order: Username, Timeout Socket, FramebufferSize, NoOfClients, Stream.
	 */
	public static void main (String[] args) throws SocketException, UnknownHostException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (args.length < 1 || args.length > 5) {
			throw new IllegalArgumentException("<Username:String>(REQUIRED) <Timeout socket:Int> <timeoutProgram:Int> <Number of Clients:Int> <Stream number:Int[,]:(1-10)>!");
		}
		String username = args[0];
		int timeoutSocket = (args.length > 1) ? Integer.parseInt(args[1]) : 1000;
		int timeoutProgram = (args.length > 2) ? Integer.parseInt(args[2]) : 60;
		int clients = (args.length > 3) ? Integer.parseInt(args[3]) : 50;
		int stream = (args.length > 4) ? Integer.parseInt(args[4]) : 6;

		System.err.println("Username: "+username);
		System.err.println("Timeout socket: "+ timeoutSocket);
		System.err.println("Timeout program: "+ timeoutProgram);
		System.err.println("Clients: "+ clients);
		System.err.println("Stream: "+ stream);

		/* Download from the stream for TIMEOUT seconds. */
		FrameAccessor fas = startAllClients(stream, clients, timeoutSocket, username);
		int time = 0;
		while(time < timeoutProgram) {
			try {
				Thread.sleep(1000);
				time++;
				System.err.println("Time running: "+time+" of "+timeoutProgram);
			} catch (InterruptedException ignored) {}
		}
		//This will stop the statistics-calculations.
		fas.getPerformanceStatistics();

		System.err.println("Will now print result log to stdout in the following csv-order:");
		System.err.println("user,timeoutSocket,timeoutProgram,threads,stream,bandwidth,throughput,droprate-belatrix,droprate-dobby,droprate-draco,droprate-harry,latency-belatrix,latency-dobby,latency-draco,latency-harry,droprate-total,latency-total");
		System.out.print(username + ", " + timeoutSocket + ", " + timeoutProgram + ", " + clients +  ", " + stream);
		printOnlyThroughputBandWidth(fas);
		printLatencyAndDropratePerHost(fas);
		try {
			printLatencyAndDropRatePerStream(fas);
		} catch (IOException ignored) {}
		System.exit(0);
	}

	/**
	 * Will start number of clients given.
	 * @param stream    The stream that the clients belong to
	 * @param clients   Number of clients.
	 * @param timeout   The socket timeout in ms.
	 * @param username  The username used in the Client.
	 * @return  List of created
	 */
	private static FrameAccessor startAllClients(int stream, int clients, int timeout, String username) {
		List<FrameAccessor> fas = new LinkedList<>();
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();

			FrameAccessor fa = null;
			for (int i = 0; i < clients; i++) {
				String h = hosts[i % hosts.length];
				StreamServiceClient c = null;
				try {
					c = DefaultStreamServiceClient.bind(h, timeout, username);
				} catch (SocketException | UnknownHostException e) {
					e.printStackTrace();
				}
				fa = FrameInfoFactory.getInstance().getFrameAccessor(c, "stream"+stream);
			}
		return fa;
	}

	/**
	 *
	 * @param args
	 * @param fas
	 * Service == Per host. ref TA Jakob.
	 * Metric  - Level	- Unit
	 * (UDP) packet drop rate (per service)	         - transport	  - percentage (%)
	 * (average) packet latency (per service)        - transport	  - milliseconds (ms)
	 * (average) frame throughput	                   - application	- frames per second (fps)
	 * bandwidth utilization(total network footprint)- application  - bits per second (bps)
	 */
	private static void printStatistics(String[] args, List<FrameAccessor> fas) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		double bandwidth = 0.0;
		double throughput = 0.0;
		int i = 0;
		HashMap<String, Double> dropRate = new HashMap<>();
		HashMap<String, Double> latency = new HashMap<>();
		for(String h: hosts) {
			dropRate.put(h, 0.0);
			latency.put(h, 0.0);
		}

		for (FrameAccessor f : fas) {
			bandwidth  += f.getPerformanceStatistics().getBandwidthUtilization();
			throughput += f.getPerformanceStatistics().getFrameThroughput();

			Method m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketDropRate", String.class);
			updateSome(m, f, dropRate, hosts);
			m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketLatency", String.class);
			updateSome(m, f, latency, hosts);

			if (i > 0) {
				bandwidth = bandwidth / 2.0;
				throughput = throughput / 2.0;
			}
			i++;
		}

		for (String a : args) {
			System.out.print(a+";");
		}
		System.out.print(";;");
		System.out.print("bw:"+(bandwidth)+";");
		System.out.print("tp:"+(throughput)+";");
		System.out.print("dr=[");
		for (String h : hosts) {
			System.out.print(Double.toString(dropRate.get(h))+";");
		}
		System.out.print("];l=[");
		for (String h : hosts) {
			System.out.print(Double.toString(latency.get(h)) + ";");
		}
		System.out.println("]");
	}

	/**
	 * Will update a average for each value of each host of given method for a FrameAccessor.
	 * @param method	The method used to get more method.
	 * @param f				The frameAccessor.
	 * @param map			The hashmap that will be updated.
	 * @param hosts		The hosts.
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	private static void updateSome(Method method, FrameAccessor f, HashMap<String, Double> map, String[] hosts) throws InvocationTargetException, IllegalAccessException {
		for (String h : hosts) {
			double newValue = 0.0;
			try {
				newValue = (double) method.invoke(f, h);
			}catch (java.lang.reflect.InvocationTargetException e) {
				System.err.println(e.getCause()+ " "+e.getTargetException());
			}
			double currentValue = map.get(h);
			currentValue = (currentValue > 0.001) ? (newValue + currentValue) / 2.0 : newValue;
			map.replace(h, currentValue);
		}
	}

	/**
	 * Prints Throughput and bandwidth for each FrameAccessor to stdout
	 * @param fas FrameAccessors (per Stream)
	 */
	private static void printOnlyThroughputBandWidth(FrameAccessor fas){
		double bandwidth = 0.0;
		double throughput = 0.0;
		bandwidth  += fas.getPerformanceStatistics().getBandwidthUtilization();
		throughput += fas.getPerformanceStatistics().getFrameThroughput();

		System.out.print(", " + bandwidth );
		System.out.print(", " + throughput );

	}

	/**
	 * Prints Latency and drop rate per host.
	 * @param fas	FrameAccessors
	 */
	private static void printLatencyAndDropratePerHost(FrameAccessor fas) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		HashMap<String, Double> dropRate = new HashMap<>();
		HashMap<String, Double> latency = new HashMap<>();

		for(String h: hosts) {
			dropRate.put(h, 0.0);
			latency.put(h, 0.0);
		}

    Method m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketDropRate", String.class);
    updateSome(m, fas, dropRate, hosts);
    m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketLatency", String.class);
    updateSome(m, fas, latency, hosts);


		for (String h : hosts) {
			System.out.print(", " +Double.toString(dropRate.get(h)));
		}

		for (String h : hosts) {
			System.out.print(", " +Double.toString(latency.get(h)));
		}


	}

	/**
	 * Prints Latency and drop rate per stream.
	 * @param fas	List of created FrameAccessors
	 */
	private static void printLatencyAndDropRatePerStream(FrameAccessor fas) throws IOException {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		double hostsDrop = (double)hosts.length;
		double hostsLatency = (double)hosts.length;

		double streamDrop = 0.0;
		double streamLatency = 0.0;
    for(String h: hosts) {
      double temp = fas.getPerformanceStatistics().getPacketDropRate(h);
      if(temp != -1.0){
        streamDrop += temp;
      }else {
        hostsDrop--;
      }

      temp = fas.getPerformanceStatistics().getPacketLatency(h);
      if(temp != -1.0) {
        streamLatency += temp;
      } else {
        hostsLatency--;
      }
    }
    System.out.print(", " + streamDrop/hostsDrop + ", " + streamLatency/hostsLatency+ "\n");

	}

}