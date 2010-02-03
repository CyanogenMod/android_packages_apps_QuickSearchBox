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

import com.android.quicksearchbox.ui.SourcesAdapter;
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * The configuration screen for search widgets.
 */
public class SearchWidgetConfigActivity extends Activity {
    static final String TAG = "QSB.SearchWidgetConfigActivity";

    private static final String PREFS_NAME = "SearchWidgetConfig";
    private static final String WIDGET_SOURCE_PREF_PREFIX = "widget_source_";

    private int mAppWidgetId;

    private GridView mSourceList;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.widget_config);

        mSourceList = (GridView) findViewById(R.id.widget_source_list);
        mSourceList.setOnItemClickListener(new SourceClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mSourceList.setFocusable(true);
        mSourceList.setAdapter(
                new SourcesAdapter(getViewFactory(), getGlobalSuggestionsProvider()));

        Intent intent = getIntent();
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    protected void selectSource(Source source) {
        writeWidgetSourcePref(mAppWidgetId, source);
        updateWidget(source);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void updateWidget(Source source) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        SearchWidgetProvider.setupSearchWidget(this, appWidgetManager,
                mAppWidgetId, source);
    }

    private static SharedPreferences getWidgetPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private static String getSourcePrefKey(int appWidgetId) {
        return WIDGET_SOURCE_PREF_PREFIX + appWidgetId;
    }

    private void writeWidgetSourcePref(int appWidgetId, Source source) {
        String sourceName = source == null ? null : source.getFlattenedComponentName();
        SharedPreferences.Editor prefs = getWidgetPreferences(this).edit();
        prefs.putString(getSourcePrefKey(appWidgetId), sourceName);
        prefs.commit();
    }

    public static ComponentName readWidgetSourcePref(Context context, int appWidgetId) {
        SharedPreferences prefs = getWidgetPreferences(context);
        String sourceName = prefs.getString(getSourcePrefKey(appWidgetId), null);
        return sourceName == null ? null : ComponentName.unflattenFromString(sourceName);
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private SuggestionsProvider getGlobalSuggestionsProvider() {
        return getQsbApplication().getGlobalSuggestionsProvider();
    }

    private SuggestionViewFactory getViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    private class SourceClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Source source = (Source) parent.getItemAtPosition(position);
            selectSource(source);
        }
    }
}



