package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Will implement FrameAccessor.PerformanceStatics to get statics.
 */
public class PerformanceStatistics implements FrameAccessor.PerformanceStatistics {

    private HashMap<String, List<Long>> blockLatency;
    private HashMap<String, Integer> blockDropRate;
    private AtomicLong startTime = new AtomicLong(0);
    private AtomicLong stopTime = new AtomicLong(0);
    private AtomicInteger framesDownloaded;
    private int frameWidth, frameHeight;

    /**
     * Will start the time performance.
     */
    public PerformanceStatistics() {
        blockLatency = new HashMap<>();
        blockDropRate = new HashMap<>();
        framesDownloaded = new AtomicInteger(0);
        startTime();
    }

    /**
     * Get the drop rate percent over downloaded blocks for a host.
     * @param host  The given host.
     * @return      The drop rate percent for the host.
     */
    @Override
    public double getPacketDropRate(String host) {
        int successes = 0;
        int failures;
        if(blockLatency.containsKey(host)) {
            List<Long> l = blockLatency.get(host);
            successes = l.size();
        }
        failures = blockDropRate.getOrDefault(host, 0);
        if(successes == 0 && failures == 0){
            return -1.0;
        }
        else if(successes == 0){
            return 100.0;
        }
        return (double)failures / ((double)failures + (double)successes) * 100.0;
    }

    /**
     * Get the average latency for downloaded blocks for a host.
     * @param host  The given host.
     * @return      The average latency for the host.
     */
    @Override
    public double getPacketLatency(String host) {
        List<Long> l = blockLatency.getOrDefault(host, new ArrayList<>());
        long tot = 0;
        for (long element : l)
            tot += element;
        if (l.size() == 0 || tot == 0)
            return -1.0;
        return (double)tot / (double)l.size();
    }

    /**
     * Get average frame throughput per second for downloaded frames, this
     * counted for all blocks divided in Frame-size for a more precise match
     * of the fps.
     * @return The average Frame Throughput
     */
    @Override
    public double getFrameThroughput() {
        int lat = 0;
        for (List<Long> l : blockLatency.values())
            lat += l.size();
        return (double)lat / (double) frameWidth / (double) frameHeight / ((double)(stopTime.get() - startTime.get()) / 1000);
    }

    /**
     * The average bandwidth utilization in bits using average frame throughput.
     * @return  The average bandwidth.
     */
    @Override
    public double getBandwidthUtilization() {
        return (getFrameThroughput() * 16*16*3*8 * (double) frameWidth * (double) frameHeight);
    }

    /**
     * Sets the Frame dimensions.
     * @param x Width
     * @param y Height
     */
    void setDim(int x, int y){
        this.frameWidth = x;
        this.frameHeight = y;
    }

    private void startTime(){
        if (startTime.get() == 0)
            startTime.set(System.currentTimeMillis());
    }

    synchronized void stopTime() {
        if (stopTime.get() == 0) {
            stopTime.set(System.currentTimeMillis());

            /* For stderr log */
            int successes = 0;
            int failures = 0;
            for( List<Long> l: blockLatency.values() ){
                successes += l.size();
            }

            for(Integer l: blockDropRate.values()) {
                failures += l;
            }
            System.err.println("Failures: " + failures + "and Successes. " +successes);
        }
    }

    /**
     * The RRT for a downloaded block on a given host is registered.
     * @param host                  The given host.
     * @param timeInMilliseconds    The RRT.
     */
    void addPacketLatency(String host, long timeInMilliseconds){
        synchronized (this) {
            if (stopTime.get() == 0) {
                List<Long> l = blockLatency.getOrDefault(host, new ArrayList<>());
                l.add(timeInMilliseconds);
                blockLatency.put(host, l);
            }
        }
    }

    /**
     * Increment the number of timed out for a host.
     * @param host  The given host.
     */
    void addTimeOut(String host){
        if (stopTime.get() == 0) {
            int i = blockDropRate.getOrDefault(host, 0);
            blockDropRate.put(host, ++i);
        }
    }

}