package client;

import se.umu.cs._5dv186.a1.client.DefaultStreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceClient;
import se.umu.cs._5dv186.a1.client.StreamServiceDiscovery;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client {
  public static void main (String[] args) throws SocketException, UnknownHostException {
    if (args.length != 4) {
      throw new IllegalArgumentException("<Username:String> <Timeout:Int> <Threads:Int> <streams:Int[,]:(1-10)>, given:"+args.length);
    }
    String username = args[0];
    int timeout     = Integer.parseInt(args[1]);
    int threads     = Integer.parseInt(args[2]);
    int[] streams = Arrays.stream(args[3].split(",")).mapToInt(Integer::parseInt).toArray();

    System.out.println("Username: "+username);
    System.out.println("Timeout: "+timeout);
    System.out.println("Threads: "+threads);
    System.out.println("StreamArray: "+ Arrays.toString(streams));

    /* Get all info */
    final String[] hosts = StreamServiceDiscovery.SINGLETON.findHosts();
    for (Integer t: streams) {
      for (int i = 0; i < threads; i++) {
        System.out.println(i +" "+t);
        String h = hosts[i % hosts.length];
        StreamServiceClient c = DefaultStreamServiceClient.bind(h, timeout, username);
        FrameInfoFactory.getInstance().getFrameAccessor(c, "stream"+t);
      }
    }
    /*
    System.out.println("Hosts: " + Arrays.toString(hosts));
    LinkedList<StreamServiceClient> clients = new LinkedList<>();
    for (String h : hosts) {
      clients.add(DefaultStreamServiceClient.bind(h, timeout, username));
    }

    for (StreamServiceClient ssc : clients) {
      System.out.println(ssc.getHost());
      try {
        for (StreamInfo stream : ssc.listStreams())
          System.out.println("  '" + stream.getName() + "': " + stream.getLengthInFrames() + " frames, " + stream.getWidthInBlocks() + " x " + stream.getHeightInBlocks() + " blocks");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    */



  }
}