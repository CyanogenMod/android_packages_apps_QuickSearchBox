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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import java.util.List;

/**
 * Activity for setting global search preferences. Changes to search preferences trigger a broadcast
 * intent that causes all SuggestionSources objects to be updated.
 */
public class SearchSettings extends PreferenceActivity
        implements OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final boolean DBG = false;
    private static final String TAG = "SearchSettings";

    // Only used to find the preferences after inflating
    private static final String CLEAR_SHORTCUTS_PREF = "clear_shortcuts";
    private static final String SEARCH_ENGINE_SETTINGS_PREF = "search_engine_settings";
    private static final String SEARCH_SOURCES_PREF = "search_sources";

    private SourceLookup mSources;
    private ShortcutRepository mShortcuts;

    // References to the top-level preference objects
    private Preference mClearShortcutsPreference;
    private PreferenceScreen mSearchEngineSettingsPreference;
    private PreferenceGroup mSourcePreferences;

    // Dialog ids
    private static final int CLEAR_SHORTCUTS_CONFIRM_DIALOG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSources = getSources();
        mShortcuts = getQSBApplication().getShortcutRepository();
        getPreferenceManager().setSharedPreferencesName(Sources.PREFERENCES_NAME);

        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        mClearShortcutsPreference = preferenceScreen.findPreference(CLEAR_SHORTCUTS_PREF);
        mSearchEngineSettingsPreference = (PreferenceScreen) preferenceScreen.findPreference(
                SEARCH_ENGINE_SETTINGS_PREF);
        mSourcePreferences = (PreferenceGroup) getPreferenceScreen().findPreference(
                SEARCH_SOURCES_PREF);

        mClearShortcutsPreference.setOnPreferenceClickListener(this);

        updateClearShortcutsPreference();
        populateSourcePreference();
        populateSearchEnginePreference();
    }

    @Override
    protected void onDestroy() {
        mShortcuts.close();
        super.onDestroy();
    }

    private QsbApplication getQSBApplication() {
        return (QsbApplication) getApplication();
    }

    private Config getConfig() {
        return getQSBApplication().getConfig();
    }

    private SourceLookup getSources() {
        return getQSBApplication().getSources();
    }

    /**
     * Enables/disables the "Clear search shortcuts" preference depending
     * on whether there is any search history.
     */
    private void updateClearShortcutsPreference() {
        boolean hasHistory = mShortcuts.hasHistory();
        if (DBG) Log.d(TAG, "hasHistory()=" + hasHistory);
        mClearShortcutsPreference.setEnabled(hasHistory);
    }

    /**
     * Populates the preference item for the web search engine, which links to further
     * search settings.
     */
    private void populateSearchEnginePreference() {
        Intent intent = new Intent(SearchManager.INTENT_ACTION_WEB_SEARCH_SETTINGS);
        intent.setPackage(getPackageName());

        CharSequence webSearchSettingsLabel = getActivityLabel(intent);
        mSearchEngineSettingsPreference.setTitle(webSearchSettingsLabel);
        mSearchEngineSettingsPreference.setIntent(intent);
    }

    private CharSequence getActivityLabel(Intent intent) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        if (resolveInfos.size() == 0) {
            Log.e(TAG, "No web search settings activity");
            return null;
        }
        if (resolveInfos.size() > 1) {
            Log.e(TAG, "More than one web search settings activity");
            return null;
        }
        return resolveInfos.get(0).activityInfo.loadLabel(pm);
    }

    /**
     * Fills the suggestion source list.
     */
    private void populateSourcePreference() {
        for (Source source : mSources.getSources()) {
            Preference pref = createSourcePreference(source);
            if (pref != null) {
                if (DBG) Log.d(TAG, "Adding search source: " + source);
                mSourcePreferences.addPreference(pref);
            }
        }
    }

    /**
     * Adds a suggestion source to the list of suggestion source checkbox preferences.
     */
    private Preference createSourcePreference(Source source) {
        CheckBoxPreference sourcePref = new CheckBoxPreference(this);
        sourcePref.setKey(Sources.getSourceEnabledPreference(source));
        sourcePref.setDefaultValue(mSources.isTrustedSource(source));
        sourcePref.setOnPreferenceChangeListener(this);
        CharSequence label = source.getLabel();
        sourcePref.setTitle(label);
        sourcePref.setSummaryOn(source.getSettingsDescription());
        sourcePref.setSummaryOff(source.getSettingsDescription());
        return sourcePref;
    }

    /**
     * Handles clicks on the "Clear search shortcuts" preference.
     */
    public synchronized boolean onPreferenceClick(Preference preference) {
        if (preference == mClearShortcutsPreference) {
            showDialog(CLEAR_SHORTCUTS_CONFIRM_DIALOG);
            return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CLEAR_SHORTCUTS_CONFIRM_DIALOG:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.clear_shortcuts)
                        .setMessage(R.string.clear_shortcuts_prompt)
                        .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (DBG) Log.d(TAG, "Clearing history...");
                                mShortcuts.clearHistory();
                                updateClearShortcutsPreference();
                            }
                        })
                        .setNegativeButton(R.string.disagree, null).create();
            default:
                Log.e(TAG, "unknown dialog" + id);
                return null;
        }
    }
    
    /**
     * Informs our listeners (SuggestionSources objects) about the updated settings data.
     */
    private void broadcastSettingsChanged() {
        // We use a message broadcast since the listeners could be in multiple processes.
        Intent intent = new Intent(SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED);
        Log.i(TAG, "Broadcasting: " + intent);
        sendBroadcast(intent);
    }

    public synchronized boolean onPreferenceChange(Preference preference, Object newValue) {
        broadcastSettingsChanged();
        return true;
    }

}
