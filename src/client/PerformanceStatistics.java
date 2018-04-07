package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerformanceStatistics implements FrameAccessor.PerformanceStatistics {

    private HashMap<String, List<Long>> latency;
    private HashMap<String, Integer> dropRate;
    private long start;
    private long stop;
    private long frames;
    private int x, y;

    public PerformanceStatistics() {
        latency = new HashMap<>();
        dropRate = new HashMap<>();
        frames = 0;
    }

    @Override
    public double getPacketDropRate(String host) {

        int successes = 0;
        int failures;
        if(latency.containsKey(host)) {
            List<Long> l = latency.get(host);
            successes = l.size();
        }

        failures = dropRate.getOrDefault(host, 0);

        if(successes == 0){
            return 100.0;
        }

        System.out.println(" (double)successes / ((double)failures"+(double)successes+"("+successes+")" +" "+ ((double)failures));
        return (double)successes / ((double)failures + (double)successes) * 100.0;
    }

    @Override
    public double getPacketLatency(String host) {
        List<Long> l = latency.getOrDefault(host, new ArrayList<>());



        long tot = 0;
        for (long element : l) {
            tot += element;
        }

        if (l.size() == 0 || tot == 0) {
            return -1.0;
        }
        System.out.println("(double)tot / (double)l.size()"+ (double)tot +" "+ (double)l.size());
        return (double)tot / (double)l.size();
    }

    @Override
    public double getFrameThroughput() {

        return (double)frames / ((double)(stop - start) / 1000);
    }

    @Override
    public double getBandwidthUtilization() {

        return (getFrameThroughput() * 768.0 * (double)x  * (double)y);
    }

    void setDim(int x, int y){
        this.x = x;
        this.y = y;
    }

    void startTime(){
        start = System.currentTimeMillis();
    }

    void stopTime() {
        stop = System.currentTimeMillis();
    }

    void addFrame(){
        frames++;
    }

    void addPacketLatency(String host, long timeInMilliseconds){
        List<Long> l = latency.getOrDefault(host, new ArrayList<>());
        l.add(timeInMilliseconds);
        System.err.println("added latency " + host + " "+ timeInMilliseconds);
        latency.put(host, l);
    }

    void addTimeOut(String host){
        int i = dropRate.getOrDefault(host, 0);
        System.err.println("new timeout: "+host+" "+(i+1));
        dropRate.put(host, ++i);
    }

}
