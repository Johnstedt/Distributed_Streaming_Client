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

        return (double)frames / ((double)(stop - start) / 1000);
    }

    @Override
    public double getBandwidthUtilization() {

        return getFrameThroughput() * 768 * x  * y;
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

        List<Long> l;
        if(latency.containsKey(host)) {
            l = latency.get(host);
        } else {
            l = new ArrayList<>();
        }

        l.add(timeInMilliseconds);

        latency.put(host, l);
    }

    void addTimeOut(String host){

        int i = dropRate.getOrDefault(host, 0);

        dropRate.put(host, ++i);
    }

}
