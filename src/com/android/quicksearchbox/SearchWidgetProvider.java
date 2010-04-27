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

import com.android.common.Search;
import com.android.quicksearchbox.ui.CorpusViewFactory;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Search widget provider.
 *
 */
public class SearchWidgetProvider extends AppWidgetProvider {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchWidgetProvider";

    private static final String WIDGET_SEARCH_SOURCE = "launcher-widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int count = appWidgetIds.length;
        for (int i = 0; i < count; i++) {
            updateSearchWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    private void updateSearchWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        String corpusName = SearchWidgetConfigActivity.readWidgetCorpusPref(context, appWidgetId);
        Corpus corpus = corpusName == null ? null : getCorpora(context).getCorpus(corpusName);
        setupSearchWidget(context, appWidgetManager, appWidgetId, corpus);
    }

    public static void setupSearchWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Corpus corpus) {
        if (DBG) Log.d(TAG, "setupSearchWidget()");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);

        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(Search.SOURCE, WIDGET_SEARCH_SOURCE);

        // Corpus indicator
        bindCorpusIndicator(context, views, widgetAppData, corpus);

        // Hint
        CharSequence hint;
        int backgroundId;
        if (corpus == null || corpus.isWebCorpus()) {
            hint = null;
            backgroundId = R.drawable.textfield_search_empty_google;
        } else {
            hint = corpus.getHint();
            backgroundId = R.drawable.textfield_search_empty;
        }
        views.setCharSequence(R.id.search_widget_text, "setHint", hint);
        views.setInt(R.id.search_widget_text, "setBackgroundResource", backgroundId);

        // Text field click
        Intent qsbIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        qsbIntent.setPackage(context.getPackageName());
        qsbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        qsbIntent.setData(SearchActivity.getCorpusUri(corpus));
        setOnClickIntent(context, views, R.id.search_widget_text, qsbIntent);

        Intent voiceSearchIntent = getVoiceSearchIntent(context, corpus, widgetAppData);
        if (voiceSearchIntent != null) {
            setOnClickIntent(context, views, R.id.search_widget_voice_btn, voiceSearchIntent);
            views.setViewVisibility(R.id.search_widget_voice_btn, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.search_widget_voice_btn, View.GONE);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void bindCorpusIndicator(Context context, RemoteViews views,
            Bundle widgetAppData, Corpus corpus) {
        Uri sourceIconUri = getCorpusIconUri(context, corpus);
        views.setImageViewUri(R.id.corpus_indicator, sourceIconUri);

        Intent intent = new Intent(SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS);
        intent.setPackage(context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.putExtra(SearchManager.APP_DATA, widgetAppData);
        intent.setData(SearchActivity.getCorpusUri(corpus));
        setOnClickIntent(context, views, R.id.corpus_indicator, intent);
    }

    private static Intent getVoiceSearchIntent(Context context, Corpus corpus,
            Bundle widgetAppData) {
        Launcher launcher = new Launcher(context);
        if (!launcher.shouldShowVoiceSearch(corpus)) return null;
        if (corpus == null) {
            return WebCorpus.createVoiceWebSearchIntent(widgetAppData);
        } else {
            return corpus.createVoiceSearchIntent(widgetAppData);
        }
    }

    private static void setOnClickIntent(Context context, RemoteViews views,
            int viewId, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private static Uri getCorpusIconUri(Context context, Corpus corpus) {
        if (corpus == null) {
            return getCorpusViewFactory(context).getGlobalSearchIconUri();
        }
        return corpus.getCorpusIconUri();
    }

    private static QsbApplication getQsbApplication(Context context) {
        return (QsbApplication) context.getApplicationContext();
    }

    private static Corpora getCorpora(Context context) {
        return getQsbApplication(context).getCorpora();
    }

    private static CorpusViewFactory getCorpusViewFactory(Context context) {
        return getQsbApplication(context).getCorpusViewFactory();
    }

}
