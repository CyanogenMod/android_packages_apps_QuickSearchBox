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

import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;

import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Activity for setting global search preferences.
 */
public class SearchSettingsActivity extends PreferenceActivity
        implements OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchSettingsActivity";

    // Name of the preferences file used to store search preference
    public static final String PREFERENCES_NAME = "SearchSettings";

    // Intent action that opens the "Searchable Items" preference
    public static final String ACTION_SEARCHABLE_ITEMS =
            "com.android.quicksearchbox.action.SEARCHABLE_ITEMS";

    // Only used to find the preferences after inflating
    private static final String CLEAR_SHORTCUTS_PREF = "clear_shortcuts";
    private static final String SEARCH_CORPORA_PREF = "search_corpora";

    // References to the top-level preference objects
    private OkCancelPreference mClearShortcutsPreference;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(PREFERENCES_NAME);

        addPreferencesFromResource(R.xml.preferences);

        mClearShortcutsPreference =
                (OkCancelPreference) findPreference(CLEAR_SHORTCUTS_PREF);
        mClearShortcutsPreference.setListener(new OkCancelPreference.Listener() {
            @Override
            public void onDialogClosed(boolean okClicked) {
                if (okClicked) {
                    clearShortcuts();
                }
            }
        });

        Preference corporaPreference = findPreference(SEARCH_CORPORA_PREF);
        corporaPreference.setIntent(getSettings().getSearchableItemsIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClearShortcutsPreference();
    }

    private SearchSettings getSettings() {
        return QsbApplication.get(this).getSettings();
    }

    private ShortcutRepository getShortcuts() {
        return QsbApplication.get(this).getShortcutRepository();
    }

    /**
     * Enables/disables the "Clear search shortcuts" preference depending
     * on whether there is any search history.
     */
    private void updateClearShortcutsPreference() {
        getShortcuts().hasHistory(Consumers.createAsyncConsumer(mHandler, new Consumer<Boolean>() {
            @Override
            public boolean consume(Boolean hasHistory) {
                if (DBG) Log.d(TAG, "hasHistory()=" + hasHistory);
                mClearShortcutsPreference.setEnabled(hasHistory);
                return true;
            }
        }));
    }

    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    private void clearShortcuts() {
        Log.i(TAG, "Clearing shortcuts...");
        getShortcuts().clearHistory();
        mClearShortcutsPreference.setEnabled(false);
    }

}
