package com.yuAiTang.moxa.util;

import android.content.Context;
import com.yuAiTang.moxa.entity.Tracks;

public abstract class Processor {
    private final long initialDelay;
    private final long interval;

    /**
     * basic constructor of this class
     *
     * @param initialDelay unit is millisecond (ms)
     * @param interval     unit is millisecond (ms)
     */
    public Processor(long initialDelay, long interval) {
        this.initialDelay = initialDelay;
        this.interval = interval;
    }

    /**
     * loop process by ThreadPool
     *
     * @param ctx context with no certain meanings
     * @see ThreadPool
     */
    public void process(Context ctx) {
        ThreadPool.scheduleWithDelay(() -> {
            try {
                run();
            } catch (Exception e) {
                Tracker.track(ctx, Tracks.debug, e.toString());
            }
        }, initialDelay, interval);
    }

    public abstract void run() throws Exception;
}
