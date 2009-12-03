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

import com.android.quicksearchbox.framework.Searchables;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
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
        mShortcuts = ShortcutRepositoryImplLog.create(this, getConfig(), mSources);
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
        PackageManager pm = getPackageManager();

        // Try to find EnhancedGoogleSearch if installed.
        ComponentName webSearchComponent;
        try {
            webSearchComponent = ComponentName.unflattenFromString(
                    Searchables.ENHANCED_GOOGLE_SEARCH_COMPONENT_NAME);
            pm.getActivityInfo(webSearchComponent, 0);
        } catch (PackageManager.NameNotFoundException e1) {
            // EnhancedGoogleSearch is not installed. Try to get GoogleSearch.
            try {
                webSearchComponent = ComponentName.unflattenFromString(
                        Searchables.GOOGLE_SEARCH_COMPONENT_NAME);
                pm.getActivityInfo(webSearchComponent, 0);
            } catch (PackageManager.NameNotFoundException e2) {
                throw new RuntimeException("could not find a web search provider");
            }
        }

        ResolveInfo matchedInfo = findWebSearchSettingsActivity(webSearchComponent);
        if (matchedInfo == null) {
            throw new RuntimeException("could not find settings for web search provider");
        }

        Intent intent = createWebSearchSettingsIntent(matchedInfo);
        String searchEngineSettingsLabel =
                matchedInfo.activityInfo.loadLabel(pm).toString();
        mSearchEngineSettingsPreference.setTitle(searchEngineSettingsLabel);
        
        mSearchEngineSettingsPreference.setIntent(intent);
    }

    /**
     * Returns the activity in the provided package that satisfies the
     * {@link SearchManager#INTENT_ACTION_WEB_SEARCH_SETTINGS} intent, or null
     * if none.
     */
    private ResolveInfo findWebSearchSettingsActivity(ComponentName component) {
        // Get all the activities which satisfy the WEB_SEARCH_SETTINGS intent.
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(SearchManager.INTENT_ACTION_WEB_SEARCH_SETTINGS);
        List<ResolveInfo> activitiesWithWebSearchSettings = pm.queryIntentActivities(intent, 0);

        String packageName = component.getPackageName();
        String name = component.getClassName();

        // Iterate through them and see if any of them are the activity we're looking for.
        for (ResolveInfo resolveInfo : activitiesWithWebSearchSettings) {
            if (packageName.equals(resolveInfo.activityInfo.packageName)
                    && name.equals(resolveInfo.activityInfo.name)) {
                return resolveInfo;
            }
        }

        // If there is no exact match, look for one in the right package
        for (ResolveInfo resolveInfo : activitiesWithWebSearchSettings) {
            if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                return resolveInfo;
            }
        }

        return null;
    }

    /**
     * Creates an intent for accessing the web search settings from the provided ResolveInfo
     * representing an activity.
     */
    private Intent createWebSearchSettingsIntent(ResolveInfo info) {
        Intent intent = new Intent(SearchManager.INTENT_ACTION_WEB_SEARCH_SETTINGS);
        intent.setComponent(
                new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
        return intent;
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
