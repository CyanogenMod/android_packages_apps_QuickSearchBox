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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains a list of search sources.
 */
public class SearchableSources implements Sources {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchableSources";

    // The number of milliseconds that source update requests are delayed to
    // allow grouping multiple requests.
    private static final long UPDATE_SOURCES_DELAY_MILLIS = 200;

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private final Context mContext;
    private final SearchManager mSearchManager;
    private boolean mLoaded;

    // All suggestion sources, by name.
    private HashMap<String, Source> mSources;

    // The web search source to use.
    private Source mWebSearchSource;

    private final Handler mUiThread;

    private Runnable mUpdateSources = new Runnable() {
        public void run() {
            mUiThread.removeCallbacks(this);
            updateSources();
            notifyDataSetChanged();
        }
    };

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public SearchableSources(Context context, Handler uiThread) {
        mContext = context;
        mUiThread = uiThread;
        mSearchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        mLoaded = false;
    }

    public Collection<Source> getSources() {
        if (!mLoaded) {
            throw new IllegalStateException("getSources(): sources not loaded.");
        }
        return mSources.values();
    }

    public Source getSource(String name) {
        return mSources.get(name);
    }

    public Source getWebSearchSource() {
        if (!mLoaded) {
            throw new IllegalStateException("getWebSearchSource(): sources not loaded.");
        }
        return mWebSearchSource;
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
                mUiThread.postDelayed(mUpdateSources, UPDATE_SOURCES_DELAY_MILLIS);
            }
        }
    };

    public void load() {
        if (mLoaded) {
            throw new IllegalStateException("load(): Already loaded.");
        }

        // Listen for searchables changes.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        intentFilter.addAction(SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);

        // update list of sources
        updateSources();

        mLoaded = true;

        notifyDataSetChanged();
    }

    public void close() {
        mContext.unregisterReceiver(mBroadcastReceiver);

        mDataSetObservable.unregisterAll();

        mSources = null;
        mLoaded = false;
    }

    /**
     * Loads the list of suggestion sources.
     */
    private void updateSources() {
        if (DBG) Log.d(TAG, "updateSources()");
        mSources = new HashMap<String,Source>();

        addSearchableSources();

        mWebSearchSource = createWebSearchSource();
        addSource(mWebSearchSource);
    }

    private void addSearchableSources() {
        List<SearchableInfo> searchables = mSearchManager.getSearchablesInGlobalSearch();
        if (searchables == null) {
            Log.e(TAG, "getSearchablesInGlobalSearch() returned null");
            return;
        }
        for (SearchableInfo searchable : searchables) {
            SearchableSource source = createSearchableSource(searchable);
            if (source != null && source.canRead()) {
                if (DBG) Log.d(TAG, "Created source " + source);
                addSource(source);
            }
        }
    }

    private void addSource(Source source) {
        mSources.put(source.getName(), source);
    }

    private Source createWebSearchSource() {
        ComponentName name = getWebSearchComponent();
        SearchableInfo webSearchable = mSearchManager.getSearchableInfo(name);
        if (webSearchable == null) {
            Log.e(TAG, "Web search source " + name + " is not searchable.");
            return null;
        }
        return createSearchableSource(webSearchable);
    }

    private ComponentName getWebSearchComponent() {
        // Looks for an activity in the current package that handles ACTION_WEB_SEARCH.
        // This indirect method is used to allow easy replacement of the web
        // search activity when extending this package.
        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        webSearchIntent.setPackage(mContext.getPackageName());
        PackageManager pm = mContext.getPackageManager();
        return webSearchIntent.resolveActivity(pm);
    }

    private SearchableSource createSearchableSource(SearchableInfo searchable) {
        if (searchable == null) return null;
        try {
            return new SearchableSource(mContext, searchable);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Source not found: " + ex);
            return null;
        }
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }
}
