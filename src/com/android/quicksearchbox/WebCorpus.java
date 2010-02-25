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

import com.android.common.Patterns;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The web search source.
 */
public class WebCorpus extends AbstractCorpus {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.WebCorpus";

    private static final String WEB_CORPUS_NAME = "web";

    private final Context mContext;

    private final Source mWebSearchSource;
    private final Source mBrowserSource;

    private final ArrayList<Source> mSources;

    public WebCorpus(Context context, Source webSearchSource, Source browserSource) {
        mContext = context;
        mWebSearchSource = webSearchSource;
        mBrowserSource = browserSource;

        mSources = new ArrayList<Source>();
        mSources.add(mWebSearchSource);
        mSources.add(mBrowserSource);
    }

    protected Context getContext() {
        return mContext;
    }

    public CharSequence getLabel() {
        return getContext().getText(R.string.corpus_label_web);
    }

    public CharSequence getHint() {
        // The web corpus uses a drawable hint instead
        return null;
    }

    private boolean isUrl(String query) {
       return Patterns.WEB_URL.matcher(query).matches();
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        return isUrl(query)? createBrowseIntent(query) : createWebSearchIntent(query, appData);
    }

    private static Intent createWebSearchIntent(String query, Bundle appData) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SearchManager.USER_QUERY, query);
        intent.putExtra(SearchManager.QUERY, query);
        if (appData != null) {
            intent.putExtra(SearchManager.APP_DATA, appData);
        }
        // TODO: Include something like this, to let the web search activity
        // know how this query was started.
        //intent.putExtra(SearchManager.SEARCH_MODE, SearchManager.MODE_GLOBAL_SEARCH_TYPED_QUERY);
        return intent;
    }

    private static Intent createBrowseIntent(String query) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String url = URLUtil.guessUrl(query);
        intent.setData(Uri.parse(url));
        return intent;
    }

    public SuggestionData createSearchShortcut(String query) {
        SuggestionData shortcut = new SuggestionData(mWebSearchSource);
        if (isUrl(query)) {
            shortcut.setIntentAction(Intent.ACTION_VIEW);
            shortcut.setIcon1(String.valueOf(R.drawable.globe));
            shortcut.setText1(query);
            // Set query so that trackball selection works
            shortcut.setSuggestionQuery(query);
            shortcut.setIntentData(URLUtil.guessUrl(query));
        } else {
            shortcut.setIntentAction(Intent.ACTION_WEB_SEARCH);
            shortcut.setIcon1(String.valueOf(R.drawable.magnifying_glass));
            shortcut.setText1(query);
            shortcut.setSuggestionQuery(query);
        }
        return shortcut;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return createVoiceWebSearchIntent(appData);
    }

    public static Intent createVoiceWebSearchIntent(Bundle appData) {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        if (appData != null) {
            intent.putExtra(SearchManager.APP_DATA, appData);
        }
        return intent;
    }

    public Drawable getCorpusIcon() {
        return getContext().getResources().getDrawable(R.drawable.corpus_icon_web);
    }

    public Uri getCorpusIconUri() {
        int resourceId = R.drawable.corpus_icon_web;
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getContext().getPackageName())
                .appendEncodedPath(String.valueOf(resourceId))
                .build();
    }

    public String getName() {
        return WEB_CORPUS_NAME;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public boolean queryAfterZeroResults() {
        return true;
    }

    public boolean voiceSearchEnabled() {
        return true;
    }

    public boolean isWebCorpus() {
        return true;
    }

    public Collection<Source> getSources() {
        return mSources;
    }

    public CharSequence getSettingsDescription() {
        return getContext().getText(R.string.corpus_description_web);
    }

    public CorpusResult getSuggestions(String query, int queryLimit) {
        // TODO: Should run web and browser queries in parallel
        SuggestionCursor webCursor = null;
        if (SearchSettings.getShowWebSuggestions(getContext())) {
            try {
                webCursor = mWebSearchSource.getSuggestions(query, queryLimit);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error querying web search source", ex);
            }
        }
        SuggestionCursor browserCursor = null;
        try {
            browserCursor = mBrowserSource.getSuggestions(query, queryLimit);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Error querying browser search source", ex);
        }

        WebResult c = new WebResult(query, webCursor, browserCursor);
        if (DBG) Log.d(TAG, "Returning " + c.getCount() + " suggestions");
        return c;
    }

    private class WebResult extends ListSuggestionCursor implements CorpusResult {

        private SuggestionCursor mWebCursor;

        private SuggestionCursor mBrowserCursor;

        public WebResult(String userQuery, SuggestionCursor webCursor,
                SuggestionCursor browserCursor) {
            super(userQuery);
            mWebCursor = webCursor;
            mBrowserCursor = browserCursor;

            if (mBrowserCursor != null && mBrowserCursor.getCount() > 0) {
                if (DBG) Log.d(TAG, "Adding browser suggestion");
                add(new SuggestionPosition(mBrowserCursor, 0));
            }

            if (mWebCursor != null) {
                int count = mWebCursor.getCount();
                for (int i = 0; i < count; i++) {
                    if (DBG) Log.d(TAG, "Adding web suggestion");
                    add(new SuggestionPosition(mWebCursor, i));
                }
            }
        }

        public Corpus getCorpus() {
            return WebCorpus.this;
        }

        @Override
        public void close() {
            super.close();
            if (mWebCursor != null) {
                mWebCursor.close();
            }
            if (mBrowserCursor != null) {
                mBrowserCursor.close();
            }
        }
    }
}
