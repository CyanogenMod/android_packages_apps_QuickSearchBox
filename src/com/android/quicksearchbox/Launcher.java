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
import android.net.Uri;
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

    /**
     * Data sent by the app that launched QSB.
     */
    public Launcher(Context context) {
        mContext = context;
    }

    /**
     * Gets the corpus to use for any searches. This is the web corpus in "All" mode,
     * and the selected corpus otherwise.
     */
    public Corpus getSearchCorpus(Corpora corpora, Corpus selectedCorpus) {
        if (selectedCorpus != null) {
            return selectedCorpus;
        } else {
            Corpus webCorpus = corpora.getWebCorpus();
            if (webCorpus == null) {
                Log.e(TAG, "No web corpus");
            }
            return webCorpus;
        }
    }

    public boolean shouldShowVoiceSearch(Corpus corpus) {
        if (corpus != null && !corpus.voiceSearchEnabled()) {
            return false;
        }
        return isVoiceSearchAvailable();
    }

    private boolean isVoiceSearchAvailable() {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        ResolveInfo ri = mContext.getPackageManager().
                resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    public Intent getSuggestionIntent(SuggestionCursor cursor, int position,
            Bundle appSearchData) {
        cursor.moveTo(position);
        Source source = cursor.getSuggestionSource();
        String action = cursor.getSuggestionIntentAction();
        // use specific action if supplied, or default action if supplied, or fixed default
        if (action == null) {
            action = source.getDefaultIntentAction();
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }
        }

        String data = cursor.getSuggestionIntentDataString();
        String query = cursor.getSuggestionQuery();
        String userQuery = cursor.getUserQuery();
        String extraData = cursor.getSuggestionIntentExtraData();

        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data != null) {
            intent.setData(Uri.parse(data));
        }
        intent.putExtra(SearchManager.USER_QUERY, userQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (appSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
        }

        intent.setComponent(cursor.getSuggestionSource().getComponentName());
        return intent;
    }

    public void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            mContext.startActivity(intent);
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

}
