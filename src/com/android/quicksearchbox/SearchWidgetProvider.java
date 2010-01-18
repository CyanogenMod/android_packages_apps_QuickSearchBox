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

    private static final boolean SHOW_SHORTCUT_IN_WIDGET = false;

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
        intent.setPackage(context.getPackageName());
        if (DBG) Log.d(TAG, "Broadcasting " + intent);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_UPDATE_SEARCH_WIDGETS.equals(action)) {
            // We requested the update. Find the widgets and update them.
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName self = new ComponentName(context, getClass());
            int[] appWidgetIds = manager.getAppWidgetIds(self);
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

        // Shortcuts
        if (SHOW_SHORTCUT_IN_WIDGET) {
            bindShortcuts(context, views);
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    private void bindShortcuts(Context context, RemoteViews views) {
        ShortcutRepository shortcutRepo = getShortcutRepository(context);
        SuggestionCursor shortcuts = shortcutRepo.getShortcutsForQuery("");
        try {
            if (shortcuts != null && shortcuts.getCount() > 0) {
                shortcuts.moveTo(0);
                RemoteViews shortcutView = new RemoteViews(context.getPackageName(),
                        R.layout.widget_suggestion);
                bindRemoteViewSuggestion(context, shortcutView, shortcuts);
                views.addView(R.id.widget_shortcuts, shortcutView);
                views.setViewVisibility(R.id.widget_shortcuts, View.VISIBLE);
            } else {
                if (DBG) Log.d(TAG, "No shortcuts, hiding drop-down.");
                views.setViewVisibility(R.id.widget_shortcuts, View.GONE);
            }
        } finally {
            if (shortcuts != null) {
                shortcuts.close();
            }
        }
    }

    private void bindSourceSelector(Context context, RemoteViews views, Bundle widgetAppData) {
        Source source = getSources(context).getLastSelectedSource();
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

    private void bindRemoteViewSuggestion(Context context, RemoteViews views,
            SuggestionCursor suggestion) {
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
        views.setOnClickPendingIntent(R.id.widget_suggestion, pendingIntent);
    }

    private QsbApplication getQsbApplication(Context context) {
        return (QsbApplication) context.getApplicationContext();
    }

    private SourceLookup getSources(Context context) {
        return getQsbApplication(context).getSources();
    }

    private ShortcutRepository getShortcutRepository(Context context) {
        return getQsbApplication(context).getShortcutRepository();
    }

    private SuggestionViewFactory getSuggestionViewFactory(Context context) {
        return getQsbApplication(context).getSuggestionViewFactory();
    }

}
