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

    private static final String ACTION_UPDATE_SEARCH_WIDGETS =
            "com.android.quicksearchbox.UPDATE_SEARCH_WIDGETS";

    private static final String WIDGET_SEARCH_SOURCE = "launcher-search";
    private static final String WIDGET_SEARCH_SHORTCUT_SOURCE = "launcher-search-shortcut";

    // TODO: Expose SearchManager.SOURCE instead.
    private static final String SOURCE = "source";

    /**
     * Updates all search widgets.
     */
    public static void updateSearchWidgets(Context context) {
        Intent intent = new Intent(ACTION_UPDATE_SEARCH_WIDGETS);
        intent.setComponent(myComponentName(context));
        if (DBG) Log.d(TAG, "Broadcasting " + intent);
        context.sendBroadcast(intent);
    }

    /**
     * Gets the component name for this app widget provider.
     */
    private static ComponentName myComponentName(Context context) {
        return new ComponentName(context, SearchWidgetProvider.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_UPDATE_SEARCH_WIDGETS.equals(action)) {
            // We requested the update. Find the widgets and update them.
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = manager.getAppWidgetIds(myComponentName(context));
            onUpdate(context, manager, appWidgetIds);
        } else {
            // Handle actions requested by the widget host.
            super.onReceive(context, intent);
        }
    }

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

        // Text field
        Intent qsbIntent = new Intent(Intent.ACTION_MAIN);
        qsbIntent.setClass(context, SearchActivity.class);
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

        // Show the top shortcut
        ShortcutRepository shortcutRepo = getShortcutRepository(context);
        SuggestionCursor shortcuts = shortcutRepo.getShortcutsForQuery("");
        try {
            if (shortcuts != null && shortcuts.getCount() > 0) {
                shortcuts.moveTo(0);

                // TODO: Eclair 2.1 adds RemoteViews.addView(), use that instead,
                // so that we can show multiple suggestions.
                // RemoteViews shortcutView = new RemoteViews(context.getPackageName(), R.layout.suggestion);
                // views.addView(R.id.widget_shortcuts, shortcutView);
                // bindRemoteViewSuggestion(context, shortcutView, shortcuts);

                bindRemoteViewSuggestion(context, views, shortcuts);
            } else {
                clearRemoteViewSuggestion(context, views);
            }
        } finally {
            if (shortcuts != null) {
                shortcuts.close();
            }
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    private void clearRemoteViewSuggestion(Context context, RemoteViews views) {
// TODO: These throw exceptions because of http://b/issue?id=2301242
//        setText1(views, null);
//        setIcon1(views, null);
//        setPendingIntent(views, null);
    }

    private void bindRemoteViewSuggestion(Context context, RemoteViews views, SuggestionCursor suggestion) {
        CharSequence text1 = suggestion.getSuggestionFormattedText1();
        CharSequence text2 = suggestion.getSuggestionFormattedText2();
        Uri icon1 = suggestion.getIconUri(suggestion.getSuggestionIcon1());
        if (icon1 == null) {
            icon1 = suggestion.getSourceIconUri();
        }
        Uri icon2 = suggestion.getIconUri(suggestion.getSuggestionIcon2());
        PendingIntent pendingIntent = getWidgetSuggestionIntent(context, suggestion);
        if (DBG) {
            Log.d(TAG, "Adding shortcut to widget: text1=" + text1 + ",text2=" + text2
                    + ",icon1=" + icon1 + ",icon2=" + icon2);
            Log.d(TAG, "    intent=" + pendingIntent);
        }
        setText1(views, text1);
        setIcon1(views, icon1);
        setPendingIntent(views, pendingIntent);
    }

    private PendingIntent getWidgetSuggestionIntent(Context context, SuggestionCursor suggestion) {
        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(SOURCE, WIDGET_SEARCH_SHORTCUT_SOURCE);
        Intent intent = suggestion.getSuggestionIntent(context, widgetAppData,
                KeyEvent.KEYCODE_UNKNOWN, null);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private void setText1(RemoteViews views, CharSequence text) {
        views.setCharSequence(R.id.text1, "setText", text);
    }

    private void setIcon1(RemoteViews views, Uri icon) {
        views.setImageViewUri(R.id.icon1, icon);
    }

    private void setPendingIntent(RemoteViews views, PendingIntent pendingIntent) {
        // TODO: Use R.id.suggestion when we switch to RemoteViews.addView()
        views.setOnClickPendingIntent(R.id.shortcut_1, pendingIntent);
    }

    private QsbApplication getQsbApplication(Context context) {
        return (QsbApplication) context.getApplicationContext();
    }

    private ShortcutRepository getShortcutRepository(Context context) {
        return getQsbApplication(context).getShortcutRepository();
    }

}
