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

    public void logStart(int latency, String intentSource, Source currentSearchSource,
            ArrayList<Source> orderedSources) {
        String packageName = mContext.getPackageName();
        int version = mVersionCode;
        // TODO: Add more info to startMethod
        String startMethod = intentSource;
        String currentSource = getSourceLogName(currentSearchSource);
        String enabledSources = getSourceLogNames(orderedSources);
        if (DBG){
            debug("qsb_start", packageName, version, startMethod, latency,
                    currentSource, enabledSources);
        }
        EventLogTags.writeQsbStart(packageName, version, startMethod,
                latency, currentSource, enabledSources);
    }

    public void logSuggestionClick(int position,
            SuggestionCursor suggestionCursor, ArrayList<Source> queriedSources) {
        String suggestions = getSuggestions(suggestionCursor);
        String sources = getSourceLogNames(queriedSources);
        int numChars = suggestionCursor.getUserQuery().length();
        EventLogTags.writeQsbClick(position, suggestions, sources, numChars);
    }

    public void logSearch(Source searchSource, int startMethod, int numChars) {
        String sourceName = getSourceLogName(searchSource);
        EventLogTags.writeQsbSearch(sourceName, startMethod, numChars);
    }

    public void logVoiceSearch(Source searchSource) {
        String sourceName = getSourceLogName(searchSource);
        EventLogTags.writeQsbVoiceSearch(sourceName);
    }

    public void logExit(SuggestionCursor suggestionCursor) {
        String suggestions = getSuggestions(suggestionCursor);
        EventLogTags.writeQsbExit(suggestions);
    }

    public void logWebLatency() {
        
    }

    private String getSourceLogName(Source source) {
        if (source == null) return null;
        return source.getLogName();
    }

    private String getSuggestions(SuggestionCursor suggestionCursor) {
        StringBuilder sb = new StringBuilder();
        final int count = suggestionCursor.getCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(LIST_SEPARATOR);
            suggestionCursor.moveTo(i);
            sb.append(suggestionCursor.getLogName());
        }
        return sb.toString();
    }

    private String getSourceLogNames(ArrayList<Source> sources) {
        StringBuilder sb = new StringBuilder();
        final int count = sources.size();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(LIST_SEPARATOR);
            sb.append(getSourceLogName(sources.get(i)));
        }
        return sb.toString();
    }

    private void debug(String tag, Object... args) {
        Log.d(TAG, tag + "(" + TextUtils.join(",", args) + ")");
    }
}
