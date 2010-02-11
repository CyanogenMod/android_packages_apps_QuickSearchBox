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

import com.android.quicksearchbox.ui.CorporaAdapter;
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
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
    private static final String WIDGET_CORPUS_PREF_PREFIX = "widget_corpus_";

    private int mAppWidgetId;

    private GridView mCorpusList;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.widget_config);

        mCorpusList = (GridView) findViewById(R.id.widget_corpus_list);
        mCorpusList.setOnItemClickListener(new SourceClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mCorpusList.setFocusable(true);
        mCorpusList.setAdapter(new CorporaAdapter(getViewFactory(), getCorpora(),
                getCorpusRanker()));

        Intent intent = getIntent();
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    protected void selectCorpus(Corpus corpus) {
        writeWidgetCorpusPref(mAppWidgetId, corpus);
        updateWidget(corpus);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void updateWidget(Corpus corpus) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        SearchWidgetProvider.setupSearchWidget(this, appWidgetManager,
                mAppWidgetId, corpus);
    }

    private static SharedPreferences getWidgetPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private static String getCorpusPrefKey(int appWidgetId) {
        return WIDGET_CORPUS_PREF_PREFIX + appWidgetId;
    }

    private void writeWidgetCorpusPref(int appWidgetId, Corpus corpus) {
        String corpusName = corpus == null ? null : corpus.getName();
        SharedPreferences.Editor prefs = getWidgetPreferences(this).edit();
        prefs.putString(getCorpusPrefKey(appWidgetId), corpusName);
        prefs.commit();
    }

    public static String readWidgetCorpusPref(Context context, int appWidgetId) {
        SharedPreferences prefs = getWidgetPreferences(context);
        return prefs.getString(getCorpusPrefKey(appWidgetId), null);
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private SuggestionViewFactory getViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    private class SourceClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Corpus corpus = (Corpus) parent.getItemAtPosition(position);
            selectCorpus(corpus);
        }
    }
}
