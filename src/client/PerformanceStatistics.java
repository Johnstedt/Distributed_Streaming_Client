package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerformanceStatistics implements FrameAccessor.PerformanceStatistics {

    private HashMap<String, List<Long>> latency;
    private HashMap<String, Integer> dropRate;

    public PerformanceStatistics() {
        latency = new HashMap<>();
        dropRate = new HashMap<>();
    }


    @Override
    public double getPacketDropRate(String host) {

        int successes;
        int failures;
        if(latency.containsKey(host)) {
            List<Long> l = latency.get(host);
            successes = l.size();
        } else {
            successes = 0;
        }

        if(dropRate.containsKey(host)){
            failures = dropRate.get(host);
        } else {
            failures = 0;
        }

        if(failures+successes == 0){
            return -1;
        }

        System.out.println("FAILURES: "+failures);
        System.out.println("SUCCESSESS: "+successes);

        return (double)successes / ((double)failures + (double)successes) * 100;
    }

    @Override
    public double getPacketLatency(String host) {
        List<Long> l = latency.get(host);

        long tot = 0;
        for (long element : l) {
            tot += element;
        }

        return (double)tot / l.size();
    }

    @Override
    public double getFrameThroughput() {
        return 0;
    }

    @Override
    public double getBandwidthUtilization() {
        return 0;
    }

    public void addPacketLatency(String host, long timeInMilliseconds){

        List<Long> l;
        if(latency.containsKey(host)) {
            l = latency.get(host);
        } else {
            l = new ArrayList<>();
        }

        l.add(timeInMilliseconds);

        latency.put(host, l);
    }

    public void addTimeOut(String host){

        int i = dropRate.getOrDefault(host, 0);

        dropRate.put(host, ++i);
    }

}
