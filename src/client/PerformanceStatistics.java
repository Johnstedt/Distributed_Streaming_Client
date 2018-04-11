package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStatistics implements FrameAccessor.PerformanceStatistics {

    private HashMap<String, List<Long>> latency;
    private HashMap<String, Integer> dropRate;
    private AtomicLong start = new AtomicLong(0);
    private AtomicLong stop = new AtomicLong(0);
    private AtomicInteger frames;
    private int x, y;

    public PerformanceStatistics() {
        latency = new HashMap<>();
        dropRate = new HashMap<>();
        frames = new AtomicInteger(0);
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
        if(successes == 0 && failures == 0){
            return -1.0;
        }
        else if(successes == 0){
            return 100.0;
        }
        return (double)failures / ((double)failures + (double)successes) * 100.0;
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
        return (double)tot / (double)l.size();
    }

    @Override
    public double getFrameThroughput() {
        int lat = 0;
        for (List<Long> l : latency.values()) {
            lat += l.size();
        }
        return (double)lat / (double)x / (double)y / ((double)(stop.get() - start.get()) / 1000);
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
        if (start.get() == 0)
            start.set(System.currentTimeMillis());
    }

    synchronized void stopTime() {
        if (stop.get() == 0) {
            stop.set(System.currentTimeMillis());

            int successes = 0;
            int failures = 0;
            for( List<Long> l: latency.values() ){
                successes += l.size();
            }

            for(Integer l: dropRate.values()) {
                failures += l;
            }

            System.err.println("Failures: " + failures + "and Successes. " +successes);

         //   System.out.println("PS.stopTime(): Stopped!");
        }
    }

    void addFrame(){
        if (stop.get() == 0)
            frames.incrementAndGet();
    }

    void addPacketLatency(String host, long timeInMilliseconds){
        synchronized (this) {
            if (stop.get() == 0) {
                List<Long> l = latency.getOrDefault(host, new ArrayList<>());
                l.add(timeInMilliseconds);
                //System.err.println("added latency " + host + " "+ timeInMilliseconds);
                latency.put(host, l);
            }
        }
    }

    void addTimeOut(String host){
        if (stop.get() == 0) {
            int i = dropRate.getOrDefault(host, 0);
            //System.err.println("new timeout: "+host+" "+(i+1));
            dropRate.put(host, ++i);
        }
    }

}