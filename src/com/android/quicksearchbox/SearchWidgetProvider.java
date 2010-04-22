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
import com.android.common.speech.Recognition;
import com.android.quicksearchbox.ui.CorpusViewFactory;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.text.Annotation;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Search widget provider.
 *
 */
public class SearchWidgetProvider extends BroadcastReceiver {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchWidgetProvider";

    /**
     * Broadcast intent action for showing the next voice search hint
     * (if voice search hints are enabled).
     */
    private static final String ACTION_NEXT_VOICE_SEARCH_HINT =
            "com.android.quicksearchbox.action.NEXT_VOICE_SEARCH_HINT";

    /**
     * Broadcast intent action for disabling voice search hints.
     */
    private static final String ACTION_CLOSE_VOICE_SEARCH_HINT =
            "com.android.quicksearchbox.action.CLOSE_VOICE_SEARCH_HINT";

    /**
     * Voice search hint update interval in milliseconds.
     */
    private static final long VOICE_SEARCH_HINT_UPDATE_INTERVAL
            = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    /**
     * Preference key used for storing the index of the next vocie search hint to show.
     */
    private static final String NEXT_VOICE_SEARCH_HINT_INDEX_PREF = "next_voice_search_hint";

    /**
     * The {@link Search#SOURCE} value used when starting searches from the search widget.
     */
    private static final String WIDGET_SEARCH_SOURCE = "launcher-widget";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG) Log.d(TAG, "onReceive(" + intent.toUri(0) + ")");
        String action = intent.getAction();
        if (ACTION_NEXT_VOICE_SEARCH_HINT.equals(action)) {
            getHintsFromVoiceSearch(context);
        } else if (ACTION_CLOSE_VOICE_SEARCH_HINT.equals(action)) {
            SearchSettings.setVoiceSearchHintsEnabled(context, false);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            updateSearchWidgets(context);
        }
    }

    /**
     * Updates all search widgets.
     */
    public static void updateSearchWidgets(Context context) {
        updateSearchWidgets(context, true, null);
    }

    private static void updateSearchWidgets(Context context, boolean updateVoiceSearchHint,
            CharSequence voiceSearchHint) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(myComponentName(context));

        boolean needsVoiceSearchHint = false;
        for (int appWidgetId : appWidgetIds) {
            SearchWidgetState state = getSearchWidgetState(context, appWidgetId, voiceSearchHint);
            state.updateWidget(context, appWidgetManager);
            needsVoiceSearchHint |= state.shouldShowVoiceSearchHint();
        }
        if (updateVoiceSearchHint) {
            scheduleVoiceSearchHintUpdates(context, needsVoiceSearchHint);
        }
    }

    /**
     * Gets the component name of this search widget provider.
     */
    private static ComponentName myComponentName(Context context) {
        String pkg = context.getPackageName();
        String cls = pkg + ".SearchWidgetProvider";
        return new ComponentName(pkg, cls);
    }

    private static SearchWidgetState getSearchWidgetState(Context context, 
            int appWidgetId, CharSequence voiceSearchHint) {
        String corpusName =
                SearchWidgetConfigActivity.readWidgetCorpusPref(context, appWidgetId);
        Corpus corpus = corpusName == null ? null : getCorpora(context).getCorpus(corpusName);
        if (DBG) {
            Log.d(TAG, "Updating appwidget " + appWidgetId + ", corpus=" + corpus
                    + ",VS hint=" + voiceSearchHint);
        }
        SearchWidgetState state = new SearchWidgetState(appWidgetId);

        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(Search.SOURCE, WIDGET_SEARCH_SOURCE);

        // Corpus indicator
        state.setCorpusIconUri(getCorpusIconUri(context, corpus));

        Intent corpusIconIntent = new Intent(SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS);
        corpusIconIntent.setPackage(context.getPackageName());
        corpusIconIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        corpusIconIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        corpusIconIntent.setData(SearchActivity.getCorpusUri(corpus));
        state.setCorpusIndicatorIntent(corpusIconIntent);

        // Query text view hint
        if (corpus == null || corpus.isWebCorpus()) {
            state.setQueryTextViewBackgroundResource(R.drawable.textfield_search_empty_google);
        } else {
            state.setQueryTextViewHint(corpus.getHint());
            state.setQueryTextViewBackgroundResource(R.drawable.textfield_search_empty);
        }

        // Text field click
        Intent qsbIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        qsbIntent.setPackage(context.getPackageName());
        qsbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        qsbIntent.setData(SearchActivity.getCorpusUri(corpus));
        state.setQueryTextViewIntent(qsbIntent);

        // Voice search button
        Intent voiceSearchIntent = getVoiceSearchIntent(context, corpus, widgetAppData);
        state.setVoiceSearchIntent(voiceSearchIntent);
        if (voiceSearchIntent != null
                && RecognizerIntent.ACTION_WEB_SEARCH.equals(voiceSearchIntent.getAction())) {
            state.setShouldShowVoiceSearchHint(true);
            state.setVoiceSearchHint(formatVoiceSearchHint(context, voiceSearchHint));
        }

        return state;
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

    private static Uri getCorpusIconUri(Context context, Corpus corpus) {
        if (corpus == null) {
            return getCorpusViewFactory(context).getGlobalSearchIconUri();
        }
        return corpus.getCorpusIconUri();
    }

    private static CharSequence formatVoiceSearchHint(Context context, CharSequence hint) {
        if (TextUtils.isEmpty(hint)) return null;
        SpannableStringBuilder spannedHint = new SpannableStringBuilder(
                context.getString(R.string.voice_search_hint_quotation_start));
        spannedHint.append(hint);
        Object[] items = spannedHint.getSpans(0, spannedHint.length(), Object.class);
        for (Object item : items) {
            if (item instanceof Annotation) {
                Annotation annotation = (Annotation) item;
                if (annotation.getKey().equals("action")
                        && annotation.getValue().equals("true")) {
                    final int start = spannedHint.getSpanStart(annotation);
                    final int end = spannedHint.getSpanEnd(annotation);
                    spannedHint.removeSpan(item);
                    spannedHint.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                }
            }
        }
        spannedHint.append(context.getString(R.string.voice_search_hint_quotation_end));
        return spannedHint;
    }

    public static void scheduleVoiceSearchHintUpdates(Context context, boolean enabled) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_NEXT_VOICE_SEARCH_HINT);
        intent.setComponent(myComponentName(context));
        PendingIntent updateHint = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.cancel(updateHint);
        if (enabled && SearchSettings.areVoiceSearchHintsEnabled(context)) {
            // Do one update immediately, and then at VOICE_SEARCH_HINT_UPDATE_INTERVAL intervals
            getHintsFromVoiceSearch(context);
            long period = VOICE_SEARCH_HINT_UPDATE_INTERVAL;
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + period, period, updateHint);
        }
    }

    /**
     * Requests an asynchronous update of the voice search hints.
     */
    private static void getHintsFromVoiceSearch(Context context) {
        if (!SearchSettings.areVoiceSearchHintsEnabled(context)) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        intent.putExtra(Recognition.EXTRA_HINT_CONTEXT, Recognition.HINT_CONTEXT_LAUNCHER);
        if (DBG) Log.d(TAG, "Broadcasting " + intent);
        context.sendOrderedBroadcast(intent, null,
                new HintReceiver(), null, Activity.RESULT_OK, null, null);
    }

    private static class HintReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() != Activity.RESULT_OK) {
                return;
            }
            ArrayList<CharSequence> hints = getResultExtras(true)
                    .getCharSequenceArrayList(Recognition.EXTRA_HINT_STRINGS);
            CharSequence hint = getNextHint(context, hints);
            updateSearchWidgets(context, false, hint);
        }
    }

    /**
     * Gets the next formatted hint, if there are any hints.
     * Must be called on the application main thread.
     *
     * @return A hint, or {@code null} if no hints are available.
     */
    private static CharSequence getNextHint(Context context, ArrayList<CharSequence> hints) {
        if (hints == null || hints.isEmpty()) return null;
        int i = getNextVoiceSearchHintIndex(context, hints.size());
        return hints.get(i);
    }

    private static int getNextVoiceSearchHintIndex(Context context, int size) {
        int i = getAndIncrementIntPreference(
                SearchSettings.getSearchPreferences(context),
                NEXT_VOICE_SEARCH_HINT_INDEX_PREF);
        return i % size;
    }

    // TODO: Could this be made atomic to avoid races?
    private static int getAndIncrementIntPreference(SharedPreferences prefs, String name) {
        int i = prefs.getInt(name, 0);
        prefs.edit().putInt(name, i + 1).commit();
        return i;
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

    private static class SearchWidgetState {
        private final int mAppWidgetId;
        private Uri mCorpusIconUri;
        private Intent mCorpusIndicatorIntent;
        private CharSequence mQueryTextViewHint;
        private int mQueryTextViewBackgroundResource;
        private Intent mQueryTextViewIntent;
        private Intent mVoiceSearchIntent;
        private boolean mShouldShowVoiceSearchHint;
        private CharSequence mVoiceSearchHint;

        public SearchWidgetState(int appWidgetId) {
            mAppWidgetId = appWidgetId;
        }

        public boolean shouldShowVoiceSearchHint() {
            return mShouldShowVoiceSearchHint;
        }

        public void setShouldShowVoiceSearchHint(boolean shouldShowVoiceSearchHint) {
            mShouldShowVoiceSearchHint = shouldShowVoiceSearchHint;
        }

        public void setCorpusIconUri(Uri corpusIconUri) {
            mCorpusIconUri = corpusIconUri;
        }

        public void setCorpusIndicatorIntent(Intent corpusIndicatorIntent) {
            mCorpusIndicatorIntent = corpusIndicatorIntent;
        }

        public void setQueryTextViewHint(CharSequence queryTextViewHint) {
            mQueryTextViewHint = queryTextViewHint;
        }

        public void setQueryTextViewBackgroundResource(int queryTextViewBackgroundResource) {
            mQueryTextViewBackgroundResource = queryTextViewBackgroundResource;
        }

        public void setQueryTextViewIntent(Intent queryTextViewIntent) {
            mQueryTextViewIntent = queryTextViewIntent;
        }

        public void setVoiceSearchIntent(Intent voiceSearchIntent) {
            mVoiceSearchIntent = voiceSearchIntent;
        }

        public void setVoiceSearchHint(CharSequence voiceSearchHint) {
            mVoiceSearchHint = voiceSearchHint;
        }

        public void updateWidget(Context context, AppWidgetManager appWidgetManager) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);
            // Corpus indicator
            views.setImageViewUri(R.id.corpus_indicator, mCorpusIconUri);
            setOnClickActivityIntent(context, views, R.id.corpus_indicator,
                    mCorpusIndicatorIntent);
            // Query TextView
            views.setCharSequence(R.id.search_widget_text, "setHint", mQueryTextViewHint);
            views.setInt(R.id.search_widget_text, "setBackgroundResource",
                    mQueryTextViewBackgroundResource);
            setOnClickActivityIntent(context, views, R.id.search_widget_text,
                    mQueryTextViewIntent);
            // Voice Search button
            if (mVoiceSearchIntent != null) {
                setOnClickActivityIntent(context, views, R.id.search_widget_voice_btn,
                        mVoiceSearchIntent);
                views.setViewVisibility(R.id.search_widget_voice_btn, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.search_widget_voice_btn, View.GONE);
            }
            // Voice Search hints
            if (mShouldShowVoiceSearchHint && !TextUtils.isEmpty(mVoiceSearchHint)) {
                views.setTextViewText(R.id.voice_search_hint_text, mVoiceSearchHint);

                Intent nextHintIntent = new Intent(ACTION_NEXT_VOICE_SEARCH_HINT);
                nextHintIntent.setComponent(myComponentName(context));
                setOnClickBroadcastIntent(context, views, R.id.voice_search_hint_text,
                        nextHintIntent);

                Intent closeHintIntent = new Intent(ACTION_CLOSE_VOICE_SEARCH_HINT);
                closeHintIntent.setComponent(myComponentName(context));
                setOnClickBroadcastIntent(context, views, R.id.voice_search_hint_close,
                        closeHintIntent);

                views.setViewVisibility(R.id.voice_search_hint, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.voice_search_hint, View.GONE);
            }
            appWidgetManager.updateAppWidget(mAppWidgetId, views);
        }

        private void setOnClickBroadcastIntent(Context context, RemoteViews views, int viewId,
                Intent intent) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            views.setOnClickPendingIntent(viewId, pendingIntent);
        }

        private void setOnClickActivityIntent(Context context, RemoteViews views, int viewId,
                Intent intent) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(viewId, pendingIntent);
        }
    }

}
