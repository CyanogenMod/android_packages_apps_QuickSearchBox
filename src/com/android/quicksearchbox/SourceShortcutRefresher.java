/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Refreshes shortcuts from their source.
 */
class SourceShortcutRefresher implements ShortcutRefresher {

    private final SourceTaskExecutor mExecutor;

    private final Set<String> mRefreshed = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Create a ShortcutRefresher that will refresh shortcuts using the given executor.
     *
     * @param executor Used to execute the tasks.
     */
    public SourceShortcutRefresher(SourceTaskExecutor executor) {
        mExecutor = executor;
    }

    /**
     * Sends off the refresher tasks.
     *
     * @param shortcuts The shortcuts to refresh.
     * @param listener Who to report back to.
     */
    public void refresh(SuggestionCursor shortcuts, final Listener listener) {
        int count = shortcuts.getCount();
        for (int i = 0; i < count; i++) {
            shortcuts.moveTo(i);
            if (shouldRefresh(shortcuts)) {
                String shortcutId = shortcuts.getShortcutId();
                Source source = shortcuts.getSuggestionSource();

                // If we can't find the source then invalidate the shortcut.
                // Otherwise, send off the refresh task.
                if (source == null) {
                    listener.onShortcutRefreshed(source, shortcutId, null);
                } else {
                    String extraData = shortcuts.getSuggestionIntentExtraData();
                    ShortcutRefreshTask refreshTask = new ShortcutRefreshTask(
                            source, shortcutId, extraData, listener);
                    mExecutor.execute(refreshTask);
                }
            }
        }
    }

    /**
     * Returns true if the given shortcut requires refreshing.
     */
    public boolean shouldRefresh(SuggestionCursor shortcut) {
        return shortcut.getShortcutId() != null
                && ! mRefreshed.contains(makeKey(shortcut));
    }

    /**
     * Indicate that the shortcut no longer requires refreshing.
     */
    public void onShortcutRefreshed(SuggestionCursor shortcut) {
        mRefreshed.add(makeKey(shortcut));
    }

    /**
     * Reset internal state.  This results in all shortcuts requiring refreshing.
     */
    public void reset() {
        mRefreshed.clear();
    }

    /**
     * Cancel any pending shortcut refresh requests.
     */
    public void cancelPendingTasks() {
        mExecutor.cancelPendingTasks();
    }

    private static String makeKey(SuggestionCursor shortcut) {
        return shortcut.getSuggestionSource().getFlattenedComponentName() + "#"
                + shortcut.getShortcutId();
    }

    /**
     * Refreshes a shortcut with a source and reports the result to a
     * {@link ShortcutRefresher.Listener}.
     */
    private class ShortcutRefreshTask implements SourceTask {
        private final Source mSource;
        private final String mShortcutId;
        private final String mExtraData;
        private final Listener mListener;

        /**
         * @param source The source that should validate the shortcut.
         * @param shortcutId The shortcut to be refreshed.
         * @param listener Who to report back to when the result is in.
         */
        ShortcutRefreshTask(Source source, String shortcutId, String extraData,
                Listener listener) {
            mSource = source;
            mShortcutId = shortcutId;
            mExtraData = extraData;
            mListener = listener;
        }

        public void run() {
            // TODO: Add latency tracking and logging.
            SuggestionCursor refreshed = mSource.refreshShortcut(mShortcutId, mExtraData);
            onShortcutRefreshed(refreshed);
            mListener.onShortcutRefreshed(mSource, mShortcutId, refreshed);
        }

    }
}
