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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A thread pool executor that has an unbounded work queue, and uses a fixed
 * maximum number of worker threads.
 *
 */
public class ThreadPoolSourceTaskExecutor extends ThreadPoolExecutor
        implements SourceTaskExecutor {

    public ThreadPoolSourceTaskExecutor(Config config, ThreadFactory threadFactory) {
        /* From ThreadPoolExecutor:
         *
         * Using an unbounded queue (for
         * example a {@link LinkedBlockingQueue} without a predefined
         * capacity) will cause new tasks to wait in the queue when all
         * corePoolSize threads are busy. Thus, no more than corePoolSize
         * threads will ever be created. (And the value of the maximumPoolSize
         * therefore doesn't have any effect.)  This may be appropriate when
         * each task is completely independent of others, so tasks cannot
         * affect each others execution; for example, in a web page server.
         * While this style of queuing can be useful in smoothing out
         * transient bursts of requests, it admits the possibility of
         * unbounded work queue growth when commands continue to arrive on
         * average faster than they can be processed.
         */
        super(config.getQueryThreadMaxPoolSize(),  // core pool size (= max size, see above)
                Integer.MAX_VALUE,  // irrelevant, see comment above
                config.getQueryThreadKeepAliveMillis(),  // irrelevant, no non-core threads created
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                threadFactory);
    }

    /**
     * Removes all unstarted tasks from the work queue.
     */
    public void cancelPendingTasks() {
        getQueue().clear();
    }

    public void close() {
        cancelPendingTasks();
        shutdown();
    }

    public void execute(SourceTask task) {
        super.execute(task);
    }
}
