package client;

import se.umu.cs._5dv186.a1.client.DefaultStreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceDiscovery;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Client {
	public static void main (String[] args) throws SocketException, UnknownHostException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (args.length != 6) {
			throw new IllegalArgumentException("<Username:String> <Timeout Socket:Int> <Timeout Program:Int> <Threads:Int> <streams-Start:Int[,]:(1-10)> <streams-End:Int[,]:(1-10)>, given:"+args.length);
		}
		String username = args[0];
    int timeout     = Integer.parseInt(args[1]);
    int time = Integer.parseInt(args[2]);
		int noOfThreads = Integer.parseInt(args[3]);
		List<Integer> streams = IntStream.rangeClosed(Integer.parseInt(args[4]), Integer.parseInt(args[5])).boxed().collect(Collectors.toList());

		System.err.println("Username: "+username);
		System.err.println("Timeout: "+timeout);
		System.err.println("Program time: "+time);
		System.err.println("Threads: "+ noOfThreads);
		System.err.println("StreamArray: "+ Arrays.toString(streams.toArray()));

		System.out.print(username + ", " + timeout + ", " + time + ", " + noOfThreads +  ", " + Arrays.toString(streams.toArray()));


		/* Get all info */
		List<FrameAccessor> fas = startAllClients(streams, noOfThreads, timeout, username);

		while(time > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//System.out.println("Time:" +time--);
			time--;
		}

		//printStatistics(args, fas);
		printOnlyThroughputBandWidth(args, fas);
		printLatencyAndDropratePerHost(args, fas);
		try {
			printLatencyAndDropRatePerStream(args, fas);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(127);
	}

	private static List<FrameAccessor> startAllClients(List<Integer> streams, int threads, int timeout, String username) {
		List<FrameAccessor> fas = new LinkedList<>();
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		for (Integer t: streams) {
			FrameAccessor fa = null;
			for (int i = 0; i < threads; i++) {
                //System.out.println("Thread :"+i +" for stream "+t);
                String h = hosts[i % hosts.length];
				StreamServiceClient c = null;
				try {
					c = DefaultStreamServiceClient.bind(h, timeout, username);
				} catch (SocketException | UnknownHostException e) {
					e.printStackTrace();
				}
				fa = FrameInfoFactory.getInstance().getFrameAccessor(c, "stream"+t);
			}
			fas.add(fa);
		}
		return fas;
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

	private static void printOnlyThroughputBandWidth(String[] args, List<FrameAccessor> fas){
		double bandwidth = 0.0;
		double throughput = 0.0;
		for (FrameAccessor f : fas){
			bandwidth  += f.getPerformanceStatistics().getBandwidthUtilization();
			throughput += f.getPerformanceStatistics().getFrameThroughput();
		}

		System.out.print(", " + bandwidth );
		System.out.print(", " + throughput );

	}

	private static void printLatencyAndDropratePerHost(String[] args, List<FrameAccessor> fas) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
		HashMap<String, Double> dropRate = new HashMap<>();
		HashMap<String, Double> latency = new HashMap<>();

		for(String h: hosts) {
			dropRate.put(h, 0.0);
			latency.put(h, 0.0);
		}

		for (FrameAccessor f : fas) {

			Method m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketDropRate", String.class);
			updateSome(m, f, dropRate, hosts);
			m = FrameAccessor.PerformanceStatistics.class.getMethod("getPacketLatency", String.class);
			updateSome(m, f, latency, hosts);

		}


		for (String h : hosts) {
			System.out.print(", " +Double.toString(dropRate.get(h)));
		}

		for (String h : hosts) {
			System.out.print(", " +Double.toString(latency.get(h)));
		}


	}

	private static void printLatencyAndDropRatePerStream(String[] args, List<FrameAccessor> fas) throws IOException {
		final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();

		double streamDrop;
		double streamLatency;

		for (FrameAccessor f : fas) {

			streamDrop = 0.0;
			streamLatency = 0.0;

			for(String h: hosts) {
				streamDrop += f.getPerformanceStatistics().getPacketDropRate(h);
				streamLatency += f.getPerformanceStatistics().getPacketLatency(h);
			}
			System.out.print(", " + streamDrop/(double)hosts.length + ", " + streamLatency/(double)hosts.length+ "\n");
		}
	}

}