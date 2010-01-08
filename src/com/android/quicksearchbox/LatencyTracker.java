/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import android.util.Log;

import java.util.HashMap;

/**
 * Tracks events in wall-clock time. 
 *
 */
public class LatencyTracker {

    private static final boolean DBG = true;

    public static final String NETWORK_ROUNDTRIP_START = "network_roundtrip_start";
    public static final String NETWORK_ROUNDTRIP_END = "network_roundtrip_end";

    /**
     * The logging tag of the component whose latency this object tracks.
     */
    private final String mLogTag;

    /**
     * Start time, in nanoseconds as returned by {@link System#nanoTime}.
     */
    private final long mStartTime;

    /**
     * Time of the last reported event. Same as {@link #mStartTime} before the first event.
     */
    private long mLastTime;

    /**
     * The time of each event that has been logged.
     */
    private final HashMap<String,Long> mTimeStamps;

    /**
     * Creates a new latency tracker.
     *
     * @param tag The logging tag of the component whose latency this object tracks.
     */
    public LatencyTracker(String tag) {
        mLogTag = tag;
        mStartTime = System.nanoTime();
        mLastTime = mStartTime;
        mTimeStamps = new HashMap<String,Long>();
    }

    public synchronized void addEvent(String event) {
        long now = System.nanoTime();
        long total = now - mStartTime;
        long diff = now - mLastTime;
        if (DBG) {
            Log.d(mLogTag, ms(total) + " ms (+" + ms(diff) + " ms): " + event);
        }
        mLastTime = now;
        mTimeStamps.put(event, now);
    }

    /**
     * Gets the best estimate of the user visible latency for the query.
     *
     * @return The latency in milliseconds.
     */
    public synchronized int getUserVisibleLatency() {
        return ms(mLastTime - mStartTime);
    }

    /**
     * Gets the best estimate of the network roundtrip latency for the query.
     *
     * @return The latency in milliseconds, or {@code -1} if the latency is unknown.
     */
    public synchronized int getNetworkRoundtripLatency() {
        Long start = mTimeStamps.get(NETWORK_ROUNDTRIP_START);
        Long end = mTimeStamps.get(NETWORK_ROUNDTRIP_END);
        if (start == null || end == null) {
            return -1;
        }
        return ms(end - start);
    }

    /**
     * Converts nanoseconds to milliseconds.
     */
    private static int ms(long ns) {
        return (int) (ns / 1000000);
    }

}
