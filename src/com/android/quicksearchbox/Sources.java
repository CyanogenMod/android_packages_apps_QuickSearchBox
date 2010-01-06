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

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Maintains the list of all suggestion sources.
 */
public class Sources implements SourceLookup {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "QSB.Sources";

    // Name of the preferences file used to store suggestion source preferences
    public static final String PREFERENCES_NAME = "SuggestionSources";

    // The key for the preference that holds the selected web search source
    public static final String WEB_SEARCH_SOURCE_PREF = "web_search_source";

    private static final int MSG_UPDATE_SOURCES = 0;

    // The number of milliseconds that source update requests are delayed to
    // allow grouping multiple requests.
    private static final long UPDATE_SOURCES_DELAY_MILLIS = 200;

    private final Context mContext;
    private final Config mConfig;
    private final SearchManager mSearchManager;
    private final SharedPreferences mPreferences;
    private boolean mLoaded;

    // Runs source updates
    private final UpdateHandler mHandler;

    // All available suggestion sources.
    private HashMap<ComponentName,Source> mSources;

    // The web search source to use. This is the source selected in the preferences,
    // or the default source if no source has been selected.
    private Source mSelectedWebSearchSource;

    // All enabled suggestion sources. This does not include the web search source.
    private ArrayList<Source> mEnabledSources;

    // Updates the inclusion of the web search provider.
    private ShowWebSuggestionsSettingChangeObserver mShowWebSuggestionsSettingChangeObserver;

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public Sources(Context context, Config config) {
        mContext = context;
        mConfig = config;
        mSearchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mLoaded = false;
        HandlerThread t = new HandlerThread("Sources.UpdateThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        t.start();
        mHandler = new UpdateHandler(t.getLooper());
    }

    /**
     * Gets all suggestion sources. This does not include any web search sources.
     *
     * @return A list of suggestion sources, including sources that are not enabled.
     *         Callers must not modify the returned list.
     */
    public synchronized Collection<Source> getSources() {
        if (!mLoaded) {
            throw new IllegalStateException("getSources(): sources not loaded.");
        }
        return mSources.values();
    }

    /** {@inheritDoc} */
    public synchronized Source getSourceByComponentName(ComponentName componentName) {
        Source source = mSources.get(componentName);
        
        // If the source was not found, back off to check the web source in case it's that.
        if (source == null) {
            if (mSelectedWebSearchSource != null &&
                    mSelectedWebSearchSource.getComponentName().equals(componentName)) {
                source = mSelectedWebSearchSource;
            }
        }
        return source;
    }

    /**
     * Gets all enabled suggestion sources.
     *
     * @return All enabled suggestion sources (does not include the web search source).
     *         Callers must not modify the returned list.
     */
    public synchronized ArrayList<Source> getEnabledSources() {
        if (!mLoaded) {
            throw new IllegalStateException("getEnabledSources(): sources not loaded.");
        }
        return mEnabledSources;
    }

    /** {@inheritDoc} */
    public synchronized Source getSelectedWebSearchSource() {
        if (!mLoaded) {
            throw new IllegalStateException("getSelectedWebSearchSource(): sources not loaded.");
        }
        return mSelectedWebSearchSource;
    }

    /**
     * Gets the preference key of the preference for whether the given source
     * is enabled. The preference is stored in the {@link #PREFERENCES_NAME}
     * preferences file.
     */
    public static String getSourceEnabledPreference(Source source) {
        return "enable_source_" + source.getComponentName().flattenToString();
    }

    // Broadcast receiver for package change notifications
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED.equals(action)
                    || SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED.equals(action)) {
                if (DBG) Log.d(TAG, "onReceive(" + intent + ")");
                // TODO: Instead of rebuilding the whole list on every change,
                // just add, remove or update the application that has changed.
                // Adding and updating seem tricky, since I can't see an easy way to list the
                // launchable activities in a given package.
                scheduleUpdateSources();
            }
        }
    };

    /* package */ void scheduleUpdateSources() {
        if (DBG) Log.d(TAG, "scheduleUpdateSources()");
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_SOURCES, UPDATE_SOURCES_DELAY_MILLIS);
    }

    private class UpdateHandler extends Handler {

        public UpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SOURCES:
                    // Remove any duplicate update messages
                    removeMessages(MSG_UPDATE_SOURCES);
                    updateSources();
                    break;
            }
        }
    }

    /**
     * After calling, clients must call {@link #close()} when done with this object.
     */
    public synchronized void load() {
        if (mLoaded) {
            throw new IllegalStateException("load(): Already loaded.");
        }

        // Listen for searchables changes.
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED));

        // Listen for search preference changes.
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED));
        
        mShowWebSuggestionsSettingChangeObserver =
                new ShowWebSuggestionsSettingChangeObserver(mHandler);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SHOW_WEB_SUGGESTIONS),
                true,
                mShowWebSuggestionsSettingChangeObserver);

        // update list of sources
        updateSources();
        mLoaded = true;
    }

    /**
     * Releases all resources used by this object. It is possible to call
     * {@link #load()} again after calling this method.
     */
    public synchronized void close() {
        if (!mLoaded) {
            throw new IllegalStateException("close(): Not loaded.");
        }
        mContext.unregisterReceiver(mBroadcastReceiver);
        mContext.getContentResolver().unregisterContentObserver(
                mShowWebSuggestionsSettingChangeObserver);

        mSources = null;
        mSelectedWebSearchSource = null;
        mEnabledSources = null;
        mLoaded = false;
    }

    /**
     * Loads the list of suggestion sources. This method is package private so that
     * it can be called efficiently from inner classes.
     */
    /* package */ synchronized void updateSources() {
        if (DBG) Log.d(TAG, "updateSources()");
        mSources = new HashMap<ComponentName,Source>();
        addExternalSources();

        mEnabledSources = findEnabledSources();
        mSelectedWebSearchSource = findWebSearchSource();
    }

    private void addExternalSources() {
        ArrayList<Source> trusted = new ArrayList<Source>();
        ArrayList<Source> untrusted = new ArrayList<Source>();
        for (SearchableInfo searchable : mSearchManager.getSearchablesInGlobalSearch()) {
            try {
                Source source = new SearchableSource(mContext, searchable);
                if (DBG) Log.d(TAG, "Created source " + source);
                if (isTrustedSource(source)) {
                    trusted.add(source);
                } else {
                    untrusted.add(source);
                }
            } catch (NameNotFoundException ex) {
                Log.e(TAG, "Searchable activity not found: " + ex.getMessage());
            }
        }
        for (Source s : trusted) {
            addSource(s);
        }
        for (Source s : untrusted) {
            addSource(s);
        }
    }

    private void addSource(Source source) {
        if (DBG) Log.d(TAG, "Adding source: " + source);
        Source old = mSources.put(source.getComponentName(), source);
        if (old != null) {
            Log.w(TAG, "Replaced source " + old + " for " + source.getComponentName());
        }
    }

    /**
     * Computes the list of enabled suggestion sources.
     */
    private ArrayList<Source> findEnabledSources() {
        ArrayList<Source> enabledSources = new ArrayList<Source>();
        for (Source source : mSources.values()) {
            if (isSourceEnabled(source)) {
                if (DBG) Log.d(TAG, "Adding enabled source " + source);
                enabledSources.add(source);
            }
        }
        return enabledSources;
    }

    private boolean isSourceEnabled(Source source) {
        boolean defaultEnabled = isTrustedSource(source);
        if (mPreferences == null) {
            Log.w(TAG, "Search preferences " + PREFERENCES_NAME + " not found.");
            return true;
        }
        String sourceEnabledPref = getSourceEnabledPreference(source);
        return mPreferences.getBoolean(sourceEnabledPref, defaultEnabled);
    }

    public synchronized boolean isTrustedSource(Source source) {
        if (source == null) return false;
        String packageName = source.getComponentName().getPackageName();
        return mConfig.isTrustedSource(packageName);
    }

    /**
     * Finds the selected web search source.
     */
    private Source findWebSearchSource() {
        Source webSearchSource = null;
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_WEB_SUGGESTIONS,
                1 /* default on until user actually changes it */) == 1) {
            SearchableInfo webSearchable = mSearchManager.getDefaultSearchableForWebSearch();
            if (webSearchable != null) {
                if (DBG) Log.d(TAG, "Adding web source " + webSearchable.getSearchActivity());
                // Construct a SearchableSource around the web search source. Allow
                // the web search source to provide a larger number of results with
                // WEB_RESULTS_OVERRIDE_LIMIT.
                try {
                    webSearchSource =
                            new SearchableSource(mContext, webSearchable, true);
                } catch (NameNotFoundException ex) {
                    Log.e(TAG, "Searchable activity not found: " + ex.getMessage());
                }
            }
        }
        return webSearchSource;
    }

    /**
     * ContentObserver which updates the list of enabled sources to include or exclude
     * the web search provider depending on the state of the
     * {@link Settings.System#SHOW_WEB_SUGGESTIONS} setting.
     */
    private class ShowWebSuggestionsSettingChangeObserver extends ContentObserver {
        public ShowWebSuggestionsSettingChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            scheduleUpdateSources();
        }
    }
}
