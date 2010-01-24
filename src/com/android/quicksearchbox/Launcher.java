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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

/**
 * Launches suggestions and searches.
 *
 */
public class Launcher {

    private static final String TAG = "Launcher";

    private final Context mContext;

    /** Data sent by the app that launched QSB. */
    private Bundle mAppSearchData = null;

    /**
     * Data sent by the app that launched QSB.
     *
     * @param appSearchData
     */
    public Launcher(Context context) {
        mContext = context;
    }

    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    public boolean isVoiceSearchAvailable() {
        Intent intent = createVoiceSearchIntent();
        ResolveInfo ri = mContext.getPackageManager().
                resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    public void startVoiceSearch() {
        launchIntent(createVoiceSearchIntent());
    }

    public void startSearch(Source source, String query) {
        if (source == null) {
            startWebSearch(query);
        } else {
            Intent intent = createSourceSearchIntent(source, query);
            launchIntent(intent);
        }
    }

    /**
     * Launches a web search.
     */
    public void startWebSearch(String query)  {
        Intent intent = createWebSearchIntent(query);
        if (intent != null) {
            launchIntent(intent);
        }
    }

    private Intent createVoiceSearchIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        // TODO: Should we include SearchManager.APP_DATA in the voice search intent?
        // SearchDialog doesn't seem to, but it would make sense.
        return intent;
    }

    // TODO: not all apps handle ACTION_SEARCH properly, e.g. ApplicationsProvider.
    // Maybe we should add a flag to searchable, so that QSB can hide the search button?
    private Intent createSourceSearchIntent(Source source, String query) {
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.setComponent(source.getComponentName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SearchManager.USER_QUERY, query);
        intent.putExtra(SearchManager.QUERY, query);
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        return intent;
    }

    private Intent createWebSearchIntent(String query) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SearchManager.USER_QUERY, query);
        intent.putExtra(SearchManager.QUERY, query);
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        // TODO: Include something like this, to let the web search activity
        // know how this query was started.
        //intent.putExtra(SearchManager.SEARCH_MODE, SearchManager.MODE_GLOBAL_SEARCH_TYPED_QUERY);
        return intent;
    }

    /**
     * Launches a suggestion.
     */
    public void launchSuggestion(SuggestionPosition suggestionPos,
            int actionKey, String actionMsg) {
        SuggestionCursor suggestion = suggestionPos.getSuggestion();
        Intent intent = suggestion.getSuggestionIntent(mContext, mAppSearchData,
                actionKey, actionMsg);
        if (intent != null) {
            launchIntent(intent);
        }
    }

    private void launchIntent(Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

}
