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

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains the list of all suggestion sources.
 */
public class SearchableCorpora implements Corpora {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = true;
    private static final String TAG = "QSB.DefaultCorpora";

    private final Context mContext;
    private final Config mConfig;
    private final Handler mUiThread;
    private final SharedPreferences mPreferences;

    private boolean mLoaded = false;

    private SearchableSources mSources;
    // Maps corpus names to corpora
    private HashMap<String,Corpus> mCorporaByName;
    // Maps sources to the corpus that contains them
    private HashMap<Source,Corpus> mCorporaBySource;
    // Enabled corpora
    private List<Corpus> mEnabledCorpora;
    private Corpus mWebCorpus;

    private boolean mShowWebSuggestions;

    // Updates the inclusion of the web search provider.
    private ShowWebSuggestionsSettingObserver mShowWebSuggestionsSettingObserver;

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public SearchableCorpora(Context context, Config config, Handler uiThread) {
        mContext = context;
        mConfig = config;
        mUiThread = uiThread;
        mPreferences = SearchSettings.getSearchPreferences(context);

        mSources = new SearchableSources(context, uiThread);
    }

    private void checkLoaded() {
        if (!mLoaded) {
            throw new IllegalStateException("corpora not loaded.");
        }
    }

    public Collection<Corpus> getAllCorpora() {
        checkLoaded();
        return Collections.unmodifiableCollection(mCorporaByName.values());
    }

    public Collection<Corpus> getEnabledCorpora() {
        checkLoaded();
        return mEnabledCorpora;
    }

    public Corpus getCorpus(String name) {
        checkLoaded();
        return mCorporaByName.get(name);
    }

    public Corpus getCorpusForSource(Source source) {
        checkLoaded();
        return mCorporaBySource.get(source);
    }

    public Source getSource(ComponentName name) {
        checkLoaded();
        return mSources.getSource(name);
    }

    /**
     * After calling, clients must call {@link #close()} when done with this object.
     */
    public void load() {
        if (mLoaded) {
            throw new IllegalStateException("load(): Already loaded.");
        }

        // Listen for web suggestion setting changes
        mShowWebSuggestionsSettingObserver =
                new ShowWebSuggestionsSettingObserver(mUiThread);
        SearchSettings.registerShowWebSuggestionsSettingObserver(mContext,
                mShowWebSuggestionsSettingObserver);
        updateWebSuggestionsSetting();

        mSources.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updateCorpora();
            }
        });

        // will cause a callback to updateCorpora()
        mSources.load();
        mLoaded = true;
    }

    /**
     * Releases all resources used by this object. It is possible to call
     * {@link #load()} again after calling this method.
     */
    public void close() {
        checkLoaded();

        SearchSettings.unregisterShowWebSuggestionsSettingObserver(mContext,
                mShowWebSuggestionsSettingObserver);

        mSources.close();
        mSources = null;
        mLoaded = false;
    }

    private void updateCorpora() {
        mCorporaByName = new HashMap<String,Corpus>();
        mCorporaBySource = new HashMap<Source,Corpus>();
        mEnabledCorpora = new ArrayList<Corpus>();

        Source webSource = mSources.getWebSearchSource();
        Source browserSource = mSources.getSource(getBrowserSearchComponent());
        mWebCorpus = new WebCorpus(mContext, webSource, browserSource);
        addCorpus(mWebCorpus);
        mCorporaBySource.put(webSource, mWebCorpus);
        mCorporaBySource.put(browserSource, mWebCorpus);

        // Create corpora for all unclaimed sources
        for (Source source : mSources.getSources()) {
            if (!mCorporaBySource.containsKey(source)) {
                Corpus corpus = new SingleSourceCorpus(source);
                addCorpus(corpus);
                mCorporaBySource.put(source, corpus);
            }
        }

        if (DBG) Log.d(TAG, "Updated corpora: " + mCorporaBySource.values());

        mEnabledCorpora = Collections.unmodifiableList(mEnabledCorpora);
    }

    private void addCorpus(Corpus corpus) {
        mCorporaByName.put(corpus.getName(), corpus);
        if (isCorpusEnabled(corpus)) {
            mEnabledCorpora.add(corpus);
        }
    }

    private ComponentName getBrowserSearchComponent() {
        String name = mContext.getString(R.string.browser_search_component);
        return TextUtils.isEmpty(name) ? null : ComponentName.unflattenFromString(name);
    }

    public boolean isCorpusEnabled(Corpus corpus) {
        if (corpus == null) return false;
        boolean defaultEnabled = isCorpusDefaultEnabled(corpus);
        String sourceEnabledPref = SearchSettings.getCorpusEnabledPreference(corpus);
        return mPreferences.getBoolean(sourceEnabledPref, defaultEnabled);
    }

    public boolean isCorpusDefaultEnabled(Corpus corpus) {
        String name = corpus.getName();
        return mConfig.isCorpusEnabledByDefault(name);
    }

    public boolean shouldShowWebSuggestions() {
        return mShowWebSuggestions;
    }

    private void updateWebSuggestionsSetting() {
        mShowWebSuggestions = SearchSettings.areWebSuggestionsEnabled(mContext);
    }

    /**
     * ContentObserver which updates the list of enabled sources to include or exclude
     * the web search provider depending on the state of the
     * {@link Settings.System#SHOW_WEB_SUGGESTIONS} setting.
     */
    private class ShowWebSuggestionsSettingObserver extends ContentObserver {
        public ShowWebSuggestionsSettingObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateWebSuggestionsSetting();
        }
    }

}
