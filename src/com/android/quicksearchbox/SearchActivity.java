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
import com.android.quicksearchbox.ui.SearchActivityView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchActivity";
    private static final boolean TRACE = false;

    private static final String SCHEME_CORPUS = "qsb.corpus";

    public static final String INTENT_ACTION_QSB_AND_SELECT_CORPUS
            = "com.android.quicksearchbox.action.QSB_AND_SELECT_CORPUS";

    // Keys for the saved instance state.
    private static final String INSTANCE_KEY_CORPUS = "corpus";
    private static final String INSTANCE_KEY_QUERY = "query";

    // Measures time from for last onCreate()/onNewIntent() call.
    private LatencyTracker mStartLatencyTracker;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    private SearchActivityView mSearchActivityView;

    private CorpusSelectionDialog mCorpusSelectionDialog;

    private CorporaObserver mCorporaObserver;

    private Corpus mCorpus;
    private Bundle mAppSearchData;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions(getQuery());
        }
    };

    private final Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
            mSearchActivityView.showInputMethodForQuery();
        }
    };

    private OnDestroyListener mDestroyListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (TRACE) startMethodTracing();
        recordStartTime();
        if (DBG) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView();

        mSearchActivityView.setMaxPromoted(getConfig().getMaxPromotedSuggestions());

        mSearchActivityView.setSearchClickListener(new SearchActivityView.SearchClickListener() {
            public boolean onSearchClicked(int method) {
                return SearchActivity.this.onSearchClicked(method);
            }
        });

        mSearchActivityView.setQueryListener(new SearchActivityView.QueryListener() {
            public void onQueryChanged() {
                updateSuggestionsBuffered();
            }
        });

        mSearchActivityView.setSuggestionClickListener(new ClickHandler());

        mSearchActivityView.setSettingsButtonClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSettingsClicked();
            }
        });

        mSearchActivityView.setCorpusIndicatorClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCorpusSelectionDialog();
            }
        });

        mSearchActivityView.setVoiceSearchButtonClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onVoiceSearchClicked();
            }
        });

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSearchActivityView.start();

        mCorporaObserver = new CorporaObserver();
        getCorpora().registerDataSetObserver(mCorporaObserver);
    }

    protected void setContentView() {
        setContentView(R.layout.search_activity);
        mSearchActivityView = (SearchActivityView) findViewById(R.id.search_activity_view);
    }

    private void startMethodTracing() {
        File traceDir = getDir("traces", 0);
        String traceFile = new File(traceDir, "qsb.trace").getAbsolutePath();
        Debug.startMethodTracing(traceFile);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) Log.d(TAG, "onNewIntent()");
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
    }

    private void recordStartTime() {
        mStartLatencyTracker = new LatencyTracker();
        mStarting = true;
        mTookAction = false;
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String corpusName = savedInstanceState.getString(INSTANCE_KEY_CORPUS);
        String query = savedInstanceState.getString(INSTANCE_KEY_QUERY);
        setCorpus(corpusName);
        setQuery(query, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.

        outState.putString(INSTANCE_KEY_CORPUS, getCorpusName());
        outState.putString(INSTANCE_KEY_QUERY, getQuery());
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        String corpusName = getCorpusNameFromUri(intent.getData());
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        boolean selectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);

        setCorpus(corpusName);
        setQuery(query, selectAll);
        mAppSearchData = appSearchData;

        if (startedIntoCorpusSelectionDialog()) {
            showCorpusSelectionDialog();
        }
    }

    public boolean startedIntoCorpusSelectionDialog() {
        return INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(getIntent().getAction());
    }

    /**
     * Removes corpus selector intent action, so that BACK works normally after
     * dismissing and reopening the corpus selector.
     */
    private void clearStartedIntoCorpusSelectionDialog() {
        Intent oldIntent = getIntent();
        if (SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(oldIntent.getAction())) {
            Intent newIntent = new Intent(oldIntent);
            newIntent.setAction(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
            setIntent(newIntent);
        }
    }

    public static Uri getCorpusUri(Corpus corpus) {
        if (corpus == null) return null;
        return new Uri.Builder()
                .scheme(SCHEME_CORPUS)
                .authority(corpus.getName())
                .build();
    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_CORPUS.equals(uri.getScheme())) return null;
        return uri.getAuthority();
    }

    private Corpus getCorpus(String sourceName) {
        if (sourceName == null) return null;
        Corpus corpus = getCorpora().getCorpus(sourceName);
        if (corpus == null) {
            Log.w(TAG, "Unknown corpus " + sourceName);
            return null;
        }
        return corpus;
    }

    private void setCorpus(String corpusName) {
        if (DBG) Log.d(TAG, "setCorpus(" + corpusName + ")");
        mCorpus = getCorpus(corpusName);

        mSearchActivityView.setCorpus(mCorpus);
    }

    private String getCorpusName() {
        return mCorpus == null ? null : mCorpus.getName();
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @VisibleForTesting
    public void setOnDestroyListener(OnDestroyListener l) {
        mDestroyListener = l;
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");
        getCorpora().unregisterDataSetObserver(mCorporaObserver);
        mSearchActivityView.destroy();
        super.onDestroy();
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed();
        }
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening the search
            // settings, or clicking a notification in the status bar.
            // TODO we should log both sets of suggestions in 2-pane mode
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSearchActivityView.clearSuggestions();
        getQsbApplication().getShortcutRefresher().reset();
        dismissCorpusSelectionDialog();
        super.onStop();
    }

    @Override
    protected void onRestart() {
        if (DBG) Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        updateSuggestionsBuffered();
        if (!isCorpusSelectionDialogShowing()) {
            mSearchActivityView.focusQueryTextView();
        }
        if (TRACE) Debug.stopMethodTracing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SearchSettings.addSearchSettingsMenuItem(this, menu);
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Launch the IME after a bit
            mHandler.postDelayed(mShowInputMethodTask, 0);
        }
    }

    protected String getQuery() {
        return mSearchActivityView.getQuery();
    }

    protected void setQuery(String query, boolean selectAll) {
        mSearchActivityView.setQuery(query, selectAll);
    }

    protected void showCorpusSelectionDialog() {
        if (mCorpusSelectionDialog == null) {
            mCorpusSelectionDialog = createCorpusSelectionDialog();
            mCorpusSelectionDialog.setOwnerActivity(this);
            mCorpusSelectionDialog.setOnDismissListener(new CorpusSelectorDismissListener());
            mCorpusSelectionDialog.setOnCorpusSelectedListener(new CorpusSelectionListener());
        }
        mCorpusSelectionDialog.show(mCorpus);
    }

    protected CorpusSelectionDialog createCorpusSelectionDialog() {
        return new CorpusSelectionDialog(this);
    }

    protected boolean isCorpusSelectionDialogShowing() {
        return mCorpusSelectionDialog != null && mCorpusSelectionDialog.isShowing();
    }

    protected void dismissCorpusSelectionDialog() {
        if (mCorpusSelectionDialog != null) {
            mCorpusSelectionDialog.dismiss();
        }
    }

    /**
     * @return true if a search was performed as a result of this click, false otherwise.
     */
    protected boolean onSearchClicked(int method) {
        String query = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) == 0) return false;

        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return false;

        mTookAction = true;

        // Log search start
        getLogger().logSearch(mCorpus, method, query.length());

        // Create shortcut
        SuggestionData searchShortcut = searchCorpus.createSearchShortcut(query);
        if (searchShortcut != null) {
            ListSuggestionCursor cursor = new ListSuggestionCursor(query);
            cursor.add(searchShortcut);
            getShortcutRepository().reportClick(cursor, 0);
        }

        // Start search
        Intent intent = searchCorpus.createSearchIntent(query, mAppSearchData);
        launchIntent(intent);
        return true;
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return;

        mTookAction = true;

        // Log voice search start
        getLogger().logVoiceSearch(searchCorpus);

        // Start voice search
        Intent intent = searchCorpus.createVoiceSearchIntent(mAppSearchData);
        launchIntent(intent);
    }

    protected void onSettingsClicked() {
        SearchSettings.launchSettings(this);
    }

    protected Corpus getSearchCorpus() {
        return mSearchActivityView.getSearchCorpus();
    }

    @Deprecated
    protected SuggestionCursor getCurrentSuggestions() {
        return mSearchActivityView.getCurrentSuggestions();
    }

    protected SuggestionCursor getCurrentSuggestions(SuggestionsAdapter adapter, int position) {
        SuggestionCursor suggestions = adapter.getCurrentSuggestions();
        if (suggestions == null) {
            return null;
        }
        int count = suggestions.getCount();
        if (position < 0 || position >= count) {
            Log.w(TAG, "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        suggestions.moveTo(position);
        return suggestions;
    }

    protected Set<Corpus> getCurrentIncludedCorpora() {
        Suggestions suggestions = mSearchActivityView.getSuggestions();
        return suggestions == null  ? null : suggestions.getIncludedCorpora();
    }

    protected void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            startActivity(intent);
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter adapter, int position) {
        SuggestionCursor suggestions = getCurrentSuggestions(adapter, position);
        if (suggestions == null) return false;

        if (DBG) Log.d(TAG, "Launching suggestion " + position);
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(position, suggestions, getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_LAUNCH);

        // Create shortcut
        getShortcutRepository().reportClick(suggestions, position);

        // Launch intent
        suggestions.moveTo(position);
        Intent intent = SuggestionUtils.getSuggestionIntent(suggestions, mAppSearchData);
        launchIntent(intent);

        return true;
    }

    protected void clickedQuickContact(SuggestionsAdapter adapter, int position) {
        SuggestionCursor suggestions = getCurrentSuggestions(adapter, position);
        if (suggestions == null) return;

        if (DBG) Log.d(TAG, "Used suggestion " + position);
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(position, suggestions, getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_QUICK_CONTACT);

        // Create shortcut
        getShortcutRepository().reportClick(suggestions, position);
    }

    protected void refineSuggestion(SuggestionsAdapter adapter, int position) {
        if (DBG) Log.d(TAG, "query refine clicked, pos " + position);
        SuggestionCursor suggestions = getCurrentSuggestions(adapter, position);
        if (suggestions == null) {
            return;
        }
        String query = suggestions.getSuggestionQuery();
        if (TextUtils.isEmpty(query)) {
            return;
        }

        // Log refine click
        getLogger().logSuggestionClick(position, suggestions, getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_REFINE);

        // Put query + space in query text view
        String queryWithSpace = query + ' ';
        setQuery(queryWithSpace, false);
        updateSuggestions(queryWithSpace);
        mSearchActivityView.focusQueryTextView();
    }

    protected boolean onSuggestionLongClicked(SuggestionsAdapter adapter, int position) {
        if (DBG) Log.d(TAG, "Long clicked on suggestion " + position);
        return false;
    }

    private void updateSuggestionsBuffered() {
        mHandler.removeCallbacks(mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        mHandler.postDelayed(mUpdateSuggestionsTask, delay);
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (mStarting) {
            mStarting = false;
            String source = getIntent().getStringExtra(Search.SOURCE);
            int latency = mStartLatencyTracker.getLatency();
            getLogger().logStart(latency, source, mCorpus, 
                    suggestions == null ? null : suggestions.getExpectedCorpora());
            getQsbApplication().onStartupComplete();
        }
    }

    private void getCorporaToQuery(Consumer<List<Corpus>> consumer) {
        if (mCorpus == null) {
            // No corpus selected, use all enabled corpora
            // TODO: This should be done asynchronously, since it can be expensive
            getCorpusRanker().getRankedCorpora(Consumers.createAsyncConsumer(mHandler, consumer));
        } else {
            List<Corpus> corpora = new ArrayList<Corpus>();
            Corpus searchCorpus = getSearchCorpus();
            // Query the selected corpus, and also the search corpus if it'
            // different (= web corpus).
            if (searchCorpus != null) corpora.add(searchCorpus);
            if (mCorpus != searchCorpus) corpora.add(mCorpus);
            consumer.consume(corpora);
        }
    }

    protected void getShortcutsForQuery(String query, Collection<Corpus> corporaToQuery,
            final Suggestions suggestions) {
        ShortcutRepository shortcutRepo = getShortcutRepository();
        if (shortcutRepo == null) return;
        Consumer<ShortcutCursor> consumer = Consumers.createAsyncCloseableConsumer(mHandler,
                new Consumer<ShortcutCursor>() {
            public boolean consume(ShortcutCursor shortcuts) {
                suggestions.setShortcuts(shortcuts);
                return true;
            }
        });
        shortcutRepo.getShortcutsForQuery(query, corporaToQuery, consumer);
    }

    protected void updateSuggestions(String untrimmedQuery) {
        final String query = CharMatcher.WHITESPACE.trimLeadingFrom(untrimmedQuery);
        if (DBG) Log.d(TAG, "getSuggestions(\""+query+"\","+mCorpus + ")");
        getQsbApplication().getSourceTaskExecutor().cancelPendingTasks();
        getCorporaToQuery(new Consumer<List<Corpus>>(){
            @Override
            public boolean consume(List<Corpus> corporaToQuery) {
                updateSuggestions(query, corporaToQuery);
                return true;
            }
        });
    }

    protected void updateSuggestions(String query, List<Corpus> corporaToQuery) {
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(
                query, corporaToQuery);
        getShortcutsForQuery(query, corporaToQuery, suggestions);

        // Log start latency if this is the first suggestions update
        gotSuggestions(suggestions);

        mSearchActivityView.setSuggestions(suggestions);
    }

    private class ClickHandler implements SuggestionClickListener {

        public void onSuggestionQuickContactClicked(SuggestionsAdapter adapter, int position) {
            clickedQuickContact(adapter, position);
        }

        public void onSuggestionClicked(SuggestionsAdapter adapter, int position) {
            launchSuggestion(adapter, position);
        }

        public boolean onSuggestionLongClicked(SuggestionsAdapter adapter, int position) {
            return SearchActivity.this.onSuggestionLongClicked(adapter, position);
        }

        public void onSuggestionQueryRefineClicked(SuggestionsAdapter adapter, int position) {
            refineSuggestion(adapter, position);
        }
    }

    private class CorpusSelectorDismissListener implements DialogInterface.OnDismissListener {
        public void onDismiss(DialogInterface dialog) {
            if (DBG) Log.d(TAG, "Corpus selector dismissed");
            clearStartedIntoCorpusSelectionDialog();
        }
    }

    private class CorpusSelectionListener
            implements CorpusSelectionDialog.OnCorpusSelectedListener {
        public void onCorpusSelected(String corpusName) {
            setCorpus(corpusName);
            updateSuggestions(getQuery());
            mSearchActivityView.focusQueryTextView();
            mSearchActivityView.showInputMethodForQuery();
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            setCorpus(getCorpusName());
            updateSuggestions(getQuery());
        }
    }

    public interface OnDestroyListener {
        void onDestroyed();
    }

}
