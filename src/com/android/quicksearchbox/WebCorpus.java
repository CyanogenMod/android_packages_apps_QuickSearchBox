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


import com.android.quicksearchbox.util.Util;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Patterns;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The web search source.
 */
public class WebCorpus extends MultiSourceCorpus {

    private static final String WEB_CORPUS_NAME = "web";

    private final Source mWebSearchSource;

    private final Source mBrowserSource;

    public WebCorpus(Context context, Executor executor,
            Source webSearchSource, Source browserSource) {
        super(context, executor, webSearchSource, browserSource);
        mWebSearchSource = webSearchSource;
        mBrowserSource = browserSource;
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

    private int getCorpusIconResource() {
        return R.drawable.corpus_icon_web;
    }

    public Drawable getCorpusIcon() {
        return getContext().getResources().getDrawable(getCorpusIconResource());
    }

    public Uri getCorpusIconUri() {
        return Util.getResourceUri(getContext(), getCorpusIconResource());
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

    public CharSequence getSettingsDescription() {
        return getContext().getText(R.string.corpus_description_web);
    }

    @Override
    protected List<Source> getSourcesToQuery(String query) {
        ArrayList<Source> sourcesToQuery = new ArrayList<Source>(2);
        if (mWebSearchSource != null
                && SearchSettings.getShowWebSuggestions(getContext())) {
            sourcesToQuery.add(mWebSearchSource);
        }
        if (mBrowserSource != null && query.length() > 0) {
            sourcesToQuery.add(mBrowserSource);
        }
        return sourcesToQuery;
    }

    @Override
    protected Result createResult(String query, ArrayList<SourceResult> results, int latency) {
        return new WebResult(query, results, latency);
    }

    protected class WebResult extends Result {

        public WebResult(String query, ArrayList<SourceResult> results, int latency) {
            super(query, results, latency);
        }

        @Override
        public void fill() {
            SourceResult webSearchResult = null;
            SourceResult browserResult = null;
            for (SourceResult result : getResults()) {
                if (result.getSource().equals(mWebSearchSource)) {
                    webSearchResult = result;
                } else {
                    browserResult = result;
                }
            }
            if (browserResult != null && browserResult.getCount() > 0) {
                add(new SuggestionPosition(browserResult, 0));
            }
            if (webSearchResult != null) {
                int count = webSearchResult.getCount();
                for (int i = 0; i < count; i++) {
                    add(new SuggestionPosition(webSearchResult, i));
                }
            }
        }

    }
}
