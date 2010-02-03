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

import com.android.quicksearchbox.ui.SearchSourceSelector;
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
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
        updateSearchWidgets(context, appWidgetManager, appWidgetIds);
    }

    /**
     * Updates a set of search widgets.
     */
    private void updateSearchWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        if (DBG) Log.d(TAG, "updateSearchWidgets()");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);

        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(SOURCE, WIDGET_SEARCH_SOURCE);

        // Source selector
        bindSourceSelector(context, views, widgetAppData);

        // Text field
        Intent qsbIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        qsbIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        PendingIntent textPendingIntent = PendingIntent.getActivity(context, 0, qsbIntent, 0);
        views.setOnClickPendingIntent(R.id.search_widget_text, textPendingIntent);

        // Voice search button. Only shown if voice search is available.
        Intent voiceSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        voiceSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        voiceSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        // TODO: Does VoiceSearch actually look at APP_DATA?
        voiceSearchIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        if (voiceSearchIntent.resolveActivity(context.getPackageManager()) != null) {
            PendingIntent voicePendingIntent =
                PendingIntent.getActivity(context, 0, voiceSearchIntent, 0);
            views.setOnClickPendingIntent(R.id.search_widget_voice_btn, voicePendingIntent);
            views.setViewVisibility(R.id.search_widget_voice_btn, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.search_widget_voice_btn, View.GONE);
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    private void bindSourceSelector(Context context, RemoteViews views, Bundle widgetAppData) {
        // TODO: Allow user to select source when adding the widget
        Source source = null;
        Uri sourceIconUri = getSourceIconUri(context, source);
        views.setImageViewUri(SearchSourceSelector.ICON_VIEW_ID, sourceIconUri);
        Intent intent = SearchSourceSelector.createIntent(null, "", widgetAppData);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(SearchSourceSelector.ICON_VIEW_ID, pendingIntent);
    }

    private Uri getSourceIconUri(Context context, Source source) {
        if (source == null) {
            return getSuggestionViewFactory(context).getGlobalSearchIconUri();
        }
        return source.getSourceIconUri();
    }

    private QsbApplication getQsbApplication(Context context) {
        return (QsbApplication) context.getApplicationContext();
    }

    private SourceLookup getSources(Context context) {
        return getQsbApplication(context).getSources();
    }

    private SuggestionViewFactory getSuggestionViewFactory(Context context) {
        return getQsbApplication(context).getSuggestionViewFactory();
    }

}
