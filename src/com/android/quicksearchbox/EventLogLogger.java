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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import java.util.ArrayList;

/**
 * Logs events to {@link EventLog}.
 */
public class EventLogLogger implements Logger {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.EventLogLogger";

    private static final char LIST_SEPARATOR = '|';

    private final Context mContext;

    private final int mVersionCode;

    public EventLogLogger(Context context) {
        mContext = context;
        String pkgName = mContext.getPackageName();
        try {
            PackageInfo pkgInfo = mContext.getPackageManager().getPackageInfo(pkgName, 0);
            mVersionCode = pkgInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            // The current package should always exist, how else could we
            // run code from it?
            throw new RuntimeException(ex);
        }
    }

    protected Context getContext() {
        return mContext;
    }

    protected int getVersionCode() {
        return mVersionCode;
    }

    public void logStart(int latency, String intentSource, Corpus corpus,
            ArrayList<Corpus> orderedCorpora) {
        String packageName = mContext.getPackageName();
        int version = mVersionCode;
        // TODO: Add more info to startMethod
        String startMethod = intentSource;
        String currentCorpus = getCorpusLogName(corpus);
        String enabledCorpora = getCorpusLogNames(orderedCorpora);
        if (DBG) {
            debug("qsb_start", packageName, version, startMethod, latency,
                    currentCorpus, enabledCorpora);
        }
        EventLogTags.writeQsbStart(packageName, version, startMethod,
                latency, currentCorpus, enabledCorpora);
    }

    public void logSuggestionClick(int position,
            SuggestionCursor suggestionCursor, ArrayList<Corpus> queriedCorpora) {
        String suggestions = getSuggestions(suggestionCursor);
        String corpora = getCorpusLogNames(queriedCorpora);
        int numChars = suggestionCursor.getUserQuery().length();
        if (DBG) {
            debug("qsb_click", position, suggestions, corpora, numChars);
        }
        EventLogTags.writeQsbClick(position, suggestions, corpora, numChars);
    }

    public void logSearch(Corpus corpus, int startMethod, int numChars) {
        String corpusName = getCorpusLogName(corpus);
        EventLogTags.writeQsbSearch(corpusName, startMethod, numChars);
    }

    public void logVoiceSearch(Corpus corpus) {
        String corpusName = getCorpusLogName(corpus);
        EventLogTags.writeQsbVoiceSearch(corpusName);
    }

    public void logExit(SuggestionCursor suggestionCursor, int numChars) {
        String suggestions = getSuggestions(suggestionCursor);
        EventLogTags.writeQsbExit(suggestions, numChars);
    }

    public void logWebLatency() {
        
    }

    private String getCorpusLogName(Corpus corpus) {
        if (corpus == null) return null;
        return corpus.getName();
    }

    private String getSuggestions(SuggestionCursor cursor) {
        StringBuilder sb = new StringBuilder();
        final int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(LIST_SEPARATOR);
            cursor.moveTo(i);
            String source = cursor.getSuggestionSource().getName();
            String type = cursor.getSuggestionLogType();
            if (type == null) type = "";
            String shortcut = cursor.isSuggestionShortcut() ? "shortcut" : "";
            sb.append(source).append(':').append(type).append(':').append(shortcut);
        }
        return sb.toString();
    }

    private String getCorpusLogNames(ArrayList<Corpus> corpora) {
        StringBuilder sb = new StringBuilder();
        final int count = corpora.size();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(LIST_SEPARATOR);
            sb.append(getCorpusLogName(corpora.get(i)));
        }
        return sb.toString();
    }

    private void debug(String tag, Object... args) {
        Log.d(TAG, tag + "(" + TextUtils.join(",", args) + ")");
    }
}
