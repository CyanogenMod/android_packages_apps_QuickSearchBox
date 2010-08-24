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
import com.android.quicksearchbox.ui.QueryTextView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.ui.SuggestionsView;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    // The string used for privateImeOptions to identify to the IME that it should not show
    // a microphone button since one already exists in the search dialog.
    // TODO: This should move to android-common or something.
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

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

    private CorpusSelectionDialog mCorpusSelectionDialog;

    protected SuggestionsAdapter mSuggestionsAdapter;

    // The list adapter for showing results other than query completion
    // suggestions
    protected SuggestionsAdapter mResultsAdapter;

    private CorporaObserver mCorporaObserver;

    protected QueryTextView mQueryTextView;
    // True if the query was empty on the previous call to updateQuery()
    protected boolean mQueryWasEmpty = true;
    protected Drawable mQueryTextEmptyBg;
    protected Drawable mQueryTextNotEmptyBg;

    protected SuggestionsView mSuggestionsView;

    // View that shows the results other than the query completions
    protected SuggestionsView mResultsView;

    protected boolean mSeparateResults;

    protected ImageButton mSearchGoButton;
    protected ImageButton mVoiceSearchButton;
    protected ImageButton mCorpusIndicator;

    protected ImageView mSettingsButton;

    private Corpus mCorpus;
    private Bundle mAppSearchData;
    private boolean mUpdateSuggestions;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions(getQuery());
        }
    };

    private final Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
            showInputMethodForQuery();
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

        SuggestListFocusListener suggestionFocusListener = new SuggestListFocusListener();
        SuggestionsViewKeyListener suggestionViewKeyListener = new SuggestionsViewKeyListener();
        InputMethodCloser imeCloser = new InputMethodCloser();

        mQueryTextView = (QueryTextView) findViewById(R.id.search_src_text);

        mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        mSuggestionsView.setOnScrollListener(imeCloser);
        mSuggestionsView.setOnKeyListener(suggestionViewKeyListener);
        mSuggestionsView.setOnFocusChangeListener(suggestionFocusListener);

        mResultsView = (SuggestionsView) findViewById(R.id.shortcuts);
        mSeparateResults = mResultsView != null;

        mSuggestionsAdapter = getQsbApplication().createSuggestionsAdapter();
        mSuggestionsAdapter.setSuggestionClickListener(new ClickHandler(mSuggestionsAdapter));
        mSuggestionsAdapter.setOnFocusChangeListener(suggestionFocusListener);

        if (mSeparateResults) {
            mResultsAdapter = getQsbApplication().createSuggestionsAdapter();
            mResultsAdapter.setSuggestionClickListener(new ClickHandler(mResultsAdapter));
            mResultsAdapter.setOnFocusChangeListener(suggestionFocusListener);
            mResultsView.setOnScrollListener(imeCloser);
            mResultsView.setOnFocusChangeListener(suggestionFocusListener);
            mResultsView.setOnKeyListener(suggestionViewKeyListener);
        }

        mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);
        mCorpusIndicator = (ImageButton) findViewById(R.id.corpus_indicator);
        mSettingsButton = (ImageView) findViewById(R.id.settings_icon);

        mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        mQueryTextView.setOnKeyListener(new QueryTextViewKeyListener());
        mQueryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());
        mQueryTextView.setSuggestionClickListener(new ClickHandler(mSuggestionsAdapter));
        mQueryTextEmptyBg = mQueryTextView.getBackground();

        if (mCorpusIndicator != null) {
            mCorpusIndicator.setOnClickListener(new CorpusIndicatorClickListener());
        }

        mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());

        mVoiceSearchButton.setOnClickListener(new VoiceSearchButtonClickListener());

        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(new SettingsButtonClickListener());
        }

        ButtonsKeyListener buttonsKeyListener = new ButtonsKeyListener();
        mSearchGoButton.setOnKeyListener(buttonsKeyListener);
        mVoiceSearchButton.setOnKeyListener(buttonsKeyListener);
        if (mCorpusIndicator != null) {
            mCorpusIndicator.setOnKeyListener(buttonsKeyListener);
        }

        mUpdateSuggestions = true;

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        mSuggestionsAdapter.registerDataSetObserver(new SuggestionsObserver());

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSuggestionsView.setAdapter(mSuggestionsAdapter);

        if (mSeparateResults) {
            mResultsAdapter.registerDataSetObserver(new SuggestionsObserver());
            mResultsView.setAdapter(mResultsAdapter);
        }
        mCorporaObserver = new CorporaObserver();
        getCorpora().registerDataSetObserver(mCorporaObserver);
    }

    protected void setContentView() {
        setContentView(R.layout.search_activity);
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
        Drawable sourceIcon;
        if (mCorpus == null) {
            sourceIcon = getCorpusViewFactory().getGlobalSearchIcon();
        } else {
            sourceIcon = mCorpus.getCorpusIcon();
        }
        if (separateResults()) {
            mSuggestionsAdapter.setPromoter(createSuggestionsPromoter(null));
            mResultsAdapter.setCorpus(mCorpus);
            mResultsAdapter.setPromoter(createResultsPromoter(mCorpus));
        } else {
            mSuggestionsAdapter.setCorpus(mCorpus);
            mSuggestionsAdapter.setPromoter(createSuggestionsPromoter(mCorpus));
        }
        if (mCorpusIndicator != null) {
            mCorpusIndicator.setImageDrawable(sourceIcon);
        }

        updateUi(getQuery().length() == 0);
    }

    private Promoter createSuggestionsPromoter(Corpus corpus) {
        if (separateResults()) {
            return getQsbApplication().createWebPromoter();
        } else if (corpus == null) {
            return getQsbApplication().createBlendingPromoter();
        } else {
            return getQsbApplication().createSingleCorpusPromoter();
        }
    }

    private Promoter createResultsPromoter(Corpus corpus) {
        if (separateResults()) {
            if (corpus == null) {
                return getQsbApplication().createResultsPromoter();
            } else {
                return getQsbApplication().createSingleCorpusPromoter();
            }
        } else {
            return null;
        }
    }

    private String getCorpusName() {
        return mCorpus == null ? null : mCorpus.getName();
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    protected boolean separateResults() {
        return mSeparateResults;
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

    private CorpusViewFactory getCorpusViewFactory() {
        return getQsbApplication().getCorpusViewFactory();
    }

    private VoiceSearch getVoiceSearch() {
        return QsbApplication.get(this).getVoiceSearch();
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
        mSuggestionsView.setAdapter(null);  // closes mSuggestionsAdapter
        if (mResultsView != null) {
            mResultsView.setAdapter(null);
        }
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
        mSuggestionsAdapter.setSuggestions(null);
        if (mResultsAdapter != null) {
            mResultsAdapter.setSuggestions(null);
        }
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
            mQueryTextView.requestFocus();
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
        CharSequence q = mQueryTextView.getText();
        return q == null ? "" : q.toString();
    }

    /**
     * Sets the text in the query box. Does not update the suggestions.
     */
    private void setQuery(String query, boolean selectAll) {
        mUpdateSuggestions = false;
        mQueryTextView.setText(query);
        mQueryTextView.setTextSelection(selectAll);
        mUpdateSuggestions = true;
    }

    protected void updateUi(boolean queryEmpty) {
        updateQueryTextView(queryEmpty);
        updateSearchGoButton(queryEmpty);
        updateVoiceSearchButton(queryEmpty);
    }

    private void updateQueryTextView(boolean queryEmpty) {
        if (queryEmpty) {
            if (isSearchCorpusWeb()) {
                mQueryTextView.setBackgroundDrawable(mQueryTextEmptyBg);
                mQueryTextView.setHint(null);
            } else {
                if (mQueryTextNotEmptyBg == null) {
                    mQueryTextNotEmptyBg =
                            getResources().getDrawable(R.drawable.textfield_search_empty);
                }
                mQueryTextView.setBackgroundDrawable(mQueryTextNotEmptyBg);
                mQueryTextView.setHint(mCorpus.getHint());
            }
        } else {
            mQueryTextView.setBackgroundResource(R.drawable.textfield_search);
        }
    }

    private void updateSearchGoButton(boolean queryEmpty) {
        if (queryEmpty) {
            mSearchGoButton.setVisibility(View.GONE);
        } else {
            mSearchGoButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateVoiceSearchButton(boolean queryEmpty) {
        if (queryEmpty && getVoiceSearch().shouldShowVoiceSearch(mCorpus)) {
            mVoiceSearchButton.setVisibility(View.VISIBLE);
            mQueryTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        } else {
            mVoiceSearchButton.setVisibility(View.GONE);
            mQueryTextView.setPrivateImeOptions(null);
        }
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

    /**
     * Gets the corpus to use for any searches. This is the web corpus in "All" mode,
     * and the selected corpus otherwise.
     */
    protected Corpus getSearchCorpus() {
        if (mCorpus != null) {
            return mCorpus;
        } else {
            Corpus webCorpus = getCorpora().getWebCorpus();
            if (webCorpus == null) {
                Log.e(TAG, "No web corpus");
            }
            return webCorpus;
        }
    }

    /**
     * Checks if the corpus used for typed searches is the web corpus.
     */
    protected boolean isSearchCorpusWeb() {
        Corpus corpus = getSearchCorpus();
        return corpus != null && corpus.isWebCorpus();
    }

    @Deprecated
    protected SuggestionCursor getCurrentSuggestions() {
        return mSuggestionsAdapter.getCurrentSuggestions();
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

    private Suggestions getSuggestions() {
        return mSuggestionsAdapter.getSuggestions();
    }

    protected Set<Corpus> getCurrentIncludedCorpora() {
        Suggestions suggestions = getSuggestions();
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

    protected boolean launchSuggestion(SuggestionsAdapter adapter, int position) {
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

    protected boolean onSuggestionLongClicked(SuggestionsAdapter adapter, int position) {
        if (DBG) Log.d(TAG, "Long clicked on suggestion " + position);
        return false;
    }

    protected boolean onSuggestionKeyDown(SuggestionsAdapter adapter,
            int position, int keyCode, KeyEvent event) {
        // Treat enter or search as a click
        if (       keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_SEARCH
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            return launchSuggestion(adapter, position);
        }

        return false;
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
        mQueryTextView.requestFocus();
    }

    /**
     * Hides the input method.
     */
    protected void considerHidingInputMethod() {
        if (getConfig().isKeyboardDismissedOnScroll()) {
            mQueryTextView.hideInputMethod();
        }
    }

    protected void showInputMethodForQuery() {
        mQueryTextView.showInputMethod();
    }

    protected void onSuggestionListFocusChange(SuggestionsView suggestions, boolean focused) {
    }

    protected void onQueryTextViewFocusChange(boolean focused) {
    }

    /**
     * Hides the input method when the suggestions get focus.
     */
    private class SuggestListFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Suggestions focus change, now: " + focused);
            if (focused && v instanceof SuggestionsView) {
                SuggestionsView view = (SuggestionsView) v;
                considerHidingInputMethod();
                onSuggestionListFocusChange(view, focused);
            }
        }
    }

    private class QueryTextViewFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Query focus change, now: " + focused);
            if (focused) {
                // The query box got focus, show the input method
                showInputMethodForQuery();
            }
            onQueryTextViewFocusChange(focused);
        }
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
            if (separateResults()) {
                // In two-pane mode, we always need to query the web corpus.
                Corpus webCorpus = getCorpora().getWebCorpus();
                if (webCorpus != null && webCorpus != mCorpus) corpora.add(webCorpus);
            }
            // Query the selected corpus
            corpora.add(mCorpus);
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

        suggestions.acquire();
        mSuggestionsAdapter.setSuggestions(suggestions);
        if (mResultsAdapter != null) {
            suggestions.acquire();
            mResultsAdapter.setSuggestions(suggestions);
        }
    }

    /**
     * If the input method is in fullscreen mode, and the selector corpus
     * is All or Web, use the web search suggestions as completions.
     */
    protected void updateInputMethodSuggestions() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null || !imm.isFullscreenMode()) return;
        Suggestions suggestions = mSuggestionsAdapter.getSuggestions();
        if (suggestions == null) return;
        CompletionInfo[] completions = webSuggestionsToCompletions(suggestions);
        if (DBG) Log.d(TAG, "displayCompletions(" + Arrays.toString(completions) + ")");
        imm.displayCompletions(mQueryTextView, completions);
    }

    private CompletionInfo[] webSuggestionsToCompletions(Suggestions suggestions) {
        // TODO: This should also include include web search shortcuts
        CorpusResult cursor = suggestions.getWebResult();
        if (cursor == null) return null;
        int count = cursor.getCount();
        ArrayList<CompletionInfo> completions = new ArrayList<CompletionInfo>(count);
        boolean usingWebCorpus = isSearchCorpusWeb();
        for (int i = 0; i < count; i++) {
            cursor.moveTo(i);
            if (!usingWebCorpus || cursor.isWebSearchSuggestion()) {
                String text1 = cursor.getSuggestionText1();
                completions.add(new CompletionInfo(i, i, text1));
            }
        }
        return completions.toArray(new CompletionInfo[completions.size()]);
    }

    private boolean forwardKeyToQueryTextView(int keyCode, KeyEvent event) {
        if (!event.isSystem() && !isDpadKey(keyCode)) {
            if (DBG) Log.d(TAG, "Forwarding key to query box: " + event);
            if (mQueryTextView.requestFocus()) {
                return mQueryTextView.dispatchKeyEvent(event);
            }
        }
        return false;
    }

    private boolean isDpadKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Filters the suggestions list when the search text changes.
     */
    private class SearchTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            boolean empty = s.length() == 0;
            if (empty != mQueryWasEmpty) {
                mQueryWasEmpty = empty;
                updateUi(empty);
            }
            if (mUpdateSuggestions) {
                updateSuggestionsBuffered();
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * Handles non-text keys in the query text view.
     */
    private class QueryTextViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            // Handle IME search action key
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                // if no action was taken, consume the key event so that the keyboard
                // remains on screen.
                return !onSearchClicked(Logger.SEARCH_METHOD_KEYBOARD);
            }
            return false;
        }
    }

    /**
     * Handles key events on the search and voice search buttons,
     * by refocusing to EditText.
     */
    private class ButtonsKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    /**
     * Handles key events on the suggestions list view.
     */
    private class SuggestionsViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && v instanceof SuggestionsView) {
                SuggestionsView view = ((SuggestionsView) v);
                int position = view.getSelectedPosition();
                if (onSuggestionKeyDown(view.getAdapter(), position, keyCode, event)) {
                    return true;
                }
            }
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private class InputMethodCloser implements SuggestionsView.OnScrollListener {

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            considerHidingInputMethod();
        }
    }

    private class ClickHandler implements SuggestionClickListener {
        private final SuggestionsAdapter mAdapter;

        public ClickHandler(SuggestionsAdapter suggestionsAdapter) {
            mAdapter = suggestionsAdapter;
        }

        public void onSuggestionQuickContactClicked(int position) {
            clickedQuickContact(mAdapter, position);
        }

        public void onSuggestionClicked(int position) {
            launchSuggestion(mAdapter, position);
        }

        public boolean onSuggestionLongClicked(int position) {
            return SearchActivity.this.onSuggestionLongClicked(mAdapter, position);
        }

        public void onSuggestionQueryRefineClicked(int position) {
            refineSuggestion(mAdapter, position);
        }
    }

    /**
     * Listens for clicks on the source selector.
     */
    private class SearchGoButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onSearchClicked(Logger.SEARCH_METHOD_BUTTON);
        }
    }

    /**
     * Listens for clicks on the search button.
     */
    private class CorpusIndicatorClickListener implements View.OnClickListener {
        public void onClick(View view) {
            showCorpusSelectionDialog();
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
            mQueryTextView.requestFocus();
            showInputMethodForQuery();
        }
    }

    /**
     * Listens for clicks on the voice search button.
     */
    private class VoiceSearchButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onVoiceSearchClicked();
        }
    }

    /**
     * Listens for clicks on the settings button.
     */
    private class SettingsButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onSettingsClicked();
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            setCorpus(getCorpusName());
            updateSuggestions(getQuery());
        }
    }

    private class SuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateInputMethodSuggestions();
        }
    }

    public interface OnDestroyListener {
        void onDestroyed();
    }

}
