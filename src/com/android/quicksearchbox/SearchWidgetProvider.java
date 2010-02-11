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

import com.android.quicksearchbox.ui.CorpusIndicator;
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Search widget provider.
 *
 */
public class SearchWidgetProvider extends AppWidgetProvider {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SearchWidgetProvider";

    private static final String WIDGET_SEARCH_SOURCE = "launcher-search";

    // TODO: Expose SearchManager.SOURCE instead.
    private static final String SOURCE = "source";

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
        Corpus corpus = getCorpora(context).getCorpus(corpusName);
        setupSearchWidget(context, appWidgetManager, appWidgetId, corpus);
    }

    public static void setupSearchWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Corpus corpus) {
        if (DBG) Log.d(TAG, "setupSearchWidget()");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);

        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(SOURCE, WIDGET_SEARCH_SOURCE);

        // Corpus indicator
        bindCorpusIndicator(context, views, widgetAppData, corpus);

        // Text field
        Intent qsbIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        qsbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        qsbIntent.setData(SearchActivity.getCorpusUri(corpus));
        setOnClickIntent(context, views, R.id.search_widget_text, qsbIntent);

        // Voice search button. Only shown if voice search is available.
        // TODO: This should be Voice Search for the selected source,
        // and only show if available for that source
        Intent voiceSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        voiceSearchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        voiceSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        // TODO: Does VoiceSearch actually look at APP_DATA?
        voiceSearchIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        if (voiceSearchIntent.resolveActivity(context.getPackageManager()) != null) {
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
        views.setImageViewUri(CorpusIndicator.ICON_VIEW_ID, sourceIconUri);

        Intent intent = new Intent(SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.putExtra(SearchManager.APP_DATA, widgetAppData);
        intent.setData(SearchActivity.getCorpusUri(corpus));
        setOnClickIntent(context, views, CorpusIndicator.ICON_VIEW_ID, intent);
    }

    private static void setOnClickIntent(Context context, RemoteViews views,
            int viewId, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private static Uri getCorpusIconUri(Context context, Corpus corpus) {
        if (corpus == null) {
            return getSuggestionViewFactory(context).getGlobalSearchIconUri();
        }
        return corpus.getCorpusIconUri();
    }

    private static QsbApplication getQsbApplication(Context context) {
        return (QsbApplication) context.getApplicationContext();
    }

    private static Corpora getCorpora(Context context) {
        return getQsbApplication(context).getCorpora();
    }

    private static SuggestionViewFactory getSuggestionViewFactory(Context context) {
        return getQsbApplication(context).getSuggestionViewFactory();
    }

}
