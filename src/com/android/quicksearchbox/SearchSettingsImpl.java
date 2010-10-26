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
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Manages user settings.
 */
public class SearchSettingsImpl implements SearchSettings {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchSettingsImpl";

    // Name of the preferences file used to store search preference
    public static final String PREFERENCES_NAME = "SearchSettings";

    // Intent action that opens the "Searchable Items" preference
    private static final String ACTION_SEARCHABLE_ITEMS =
            "com.android.quicksearchbox.action.SEARCHABLE_ITEMS";

    private static final String SHOW_WEB_SUGGESTIONS_PREF = "show_web_suggestions";

    /**
     * Preference key used for storing the index of the next voice search hint to show.
     */
    private static final String NEXT_VOICE_SEARCH_HINT_INDEX_PREF = "next_voice_search_hint";

    /**
     * Preference key used to store the time at which the first voice search hint was displayed.
     */
    private static final String FIRST_VOICE_HINT_DISPLAY_TIME = "first_voice_search_hint_time";

    /**
     * Preference key for the version of voice search we last got hints from.
     */
    private static final String LAST_SEEN_VOICE_SEARCH_VERSION = "voice_search_version";

    /**
     * Prefix of per-corpus enable preference
     */
    private static final String CORPUS_ENABLED_PREF_PREFIX = "enable_corpus_";

    private final Context mContext;

    private final Config mConfig;

    public SearchSettingsImpl(Context context, Config config) {
        mContext = context;
        mConfig = config;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Config getConfig() {
        return mConfig;
    }

    public void upgradeSettingsIfNeeded() {
        upgradeShowWebSuggestionsIfNeeded();
    }

    public Intent getSearchableItemsIntent() {
        Intent intent = new Intent(ACTION_SEARCHABLE_ITEMS);
        intent.setPackage(getContext().getPackageName());
        return intent;
    }

    /**
     * Gets the preference key of the preference for whether the given corpus
     * is enabled. The preference is stored in the {@link #PREFERENCES_NAME}
     * preferences file.
     */
    public static String getCorpusEnabledPreference(Corpus corpus) {
        return CORPUS_ENABLED_PREF_PREFIX + corpus.getName();
    }

    public boolean isCorpusEnabled(Corpus corpus) {
        boolean defaultEnabled = corpus.isCorpusDefaultEnabled();
        String sourceEnabledPref = getCorpusEnabledPreference(corpus);
        return getSearchPreferences().getBoolean(sourceEnabledPref, defaultEnabled);
    }

    protected SharedPreferences getSearchPreferences() {
        return getContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public boolean getShowWebSuggestions() {
        return getSearchPreferences().getBoolean(SHOW_WEB_SUGGESTIONS_PREF, true);
    }

    /**
     * Copies value from the old deprecated SHOW_WEB_SUGGESTIONS system setting.
     */
    private void upgradeShowWebSuggestionsIfNeeded() {
        SharedPreferences prefs = getSearchPreferences();
        if (!prefs.contains(SHOW_WEB_SUGGESTIONS_PREF)) {
            // Default to true if the old setting is not set
            boolean oldValue = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SHOW_WEB_SUGGESTIONS, 1) == 1;
            prefs.edit().putBoolean(SHOW_WEB_SUGGESTIONS_PREF, oldValue).commit();
            Log.i(TAG, "Copied value from Settings.System.SHOW_WEB_SUGGESTIONS: " + oldValue);
        }
    }

    /**
     * Informs our listeners about the updated settings data.
     */
    public void broadcastSettingsChanged() {
        // We use a message broadcast since the listeners could be in multiple processes.
        Intent intent = new Intent(SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED);
        Log.i(TAG, "Broadcasting: " + intent);
        getContext().sendBroadcast(intent);
    }

    public void addMenuItems(Menu menu) {
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(R.menu.settings, menu);
        MenuItem item = menu.findItem(R.id.menu_settings);
        item.setIntent(getSearchSettingsIntent());
    }

    public void updateMenuItems(Menu menu) {
    }

    public Intent getSearchSettingsIntent() {
        Intent settings = new Intent(SearchManager.INTENT_ACTION_SEARCH_SETTINGS);
        settings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        settings.setPackage(getContext().getPackageName());
        return settings;
    }

    public int getNextVoiceSearchHintIndex(int size) {
            int i = getAndIncrementIntPreference(getSearchPreferences(),
                    NEXT_VOICE_SEARCH_HINT_INDEX_PREF);
            return i % size;
    }

    // TODO: Could this be made atomic to avoid races?
    private static int getAndIncrementIntPreference(SharedPreferences prefs, String name) {
        int i = prefs.getInt(name, 0);
        prefs.edit().putInt(name, i + 1).commit();
        return i;
    }

    public void resetVoiceSearchHintFirstSeenTime() {
        getSearchPreferences().edit()
                .putLong(FIRST_VOICE_HINT_DISPLAY_TIME, System.currentTimeMillis()).commit();
    }

    public boolean haveVoiceSearchHintsExpired(int currentVoiceSearchVersion) {
        SharedPreferences prefs = getSearchPreferences();

        if (currentVoiceSearchVersion != 0) {
            long currentTime = System.currentTimeMillis();
            int lastVoiceSearchVersion = prefs.getInt(LAST_SEEN_VOICE_SEARCH_VERSION, 0);
            long firstHintTime = prefs.getLong(FIRST_VOICE_HINT_DISPLAY_TIME, 0);
            if (firstHintTime == 0 || currentVoiceSearchVersion != lastVoiceSearchVersion) {
                prefs.edit()
                        .putInt(LAST_SEEN_VOICE_SEARCH_VERSION, currentVoiceSearchVersion)
                        .putLong(FIRST_VOICE_HINT_DISPLAY_TIME, currentTime)
                        .commit();
                firstHintTime = currentTime;
            }
            if (currentTime - firstHintTime > getConfig().getVoiceSearchHintActivePeriod()) {
                if (DBG) Log.d(TAG, "Voice seach hint period expired; not showing hints.");
                return true;
            } else {
                return false;
            }
        } else {
            if (DBG) Log.d(TAG, "Could not determine voice search version; not showing hints.");
            return true;
        }
    }

    public boolean allowWebSearchShortcuts() {
        return true;
    }

}
