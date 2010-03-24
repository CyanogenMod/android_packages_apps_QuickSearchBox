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
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionSelectionListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.ui.SuggestionsFooter;
import com.android.quicksearchbox.ui.SuggestionsView;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    private static final String INSTANCE_KEY_USER_QUERY = "query";

    // Measures time from for last onCreate()/onNewIntent() call.
    private LatencyTracker mStartLatencyTracker;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    private CorpusSelectionDialog mCorpusSelectionDialog;

    protected SuggestionsAdapter mSuggestionsAdapter;

    protected EditText mQueryTextView;
    // True if the query was empty on the previous call to updateQuery()
    protected boolean mQueryWasEmpty = true;

    protected SuggestionsView mSuggestionsView;
    protected SuggestionsFooter mSuggestionsFooter;

    protected ImageButton mSearchGoButton;
    protected ImageButton mVoiceSearchButton;
    protected ImageButton mCorpusIndicator;

    private Launcher mLauncher;

    private Corpus mCorpus;
    private Bundle mAppSearchData;
    private boolean mUpdateSuggestions;
    private String mUserQuery;
    private boolean mSelectAll;

    private Handler mHandler = new Handler();
    private Runnable mUpdateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions(getQuery());
        }
    };

    private Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
            showInputMethodForQuery();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (TRACE) startMethodTracing();
        recordStartTime();
        if (DBG) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_activity);

        mSuggestionsAdapter = getQsbApplication().createSuggestionsAdapter();

        mQueryTextView = (EditText) findViewById(R.id.search_src_text);
        mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        mSuggestionsView.setSuggestionClickListener(new ClickHandler());
        mSuggestionsView.setSuggestionSelectionListener(new SelectionHandler());
        mSuggestionsView.setInteractionListener(new InputMethodCloser());
        mSuggestionsView.setOnKeyListener(new SuggestionsViewKeyListener());
        mSuggestionsView.setOnFocusChangeListener(new SuggestListFocusListener());

        mSuggestionsFooter = getQsbApplication().createSuggestionsFooter();
        ViewGroup footerFrame = (ViewGroup) findViewById(R.id.footer);
        mSuggestionsFooter.addToContainer(footerFrame);

        mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);
        mCorpusIndicator = (ImageButton) findViewById(R.id.corpus_indicator);

        mLauncher = new Launcher(this);

        mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        mQueryTextView.setOnKeyListener(new QueryTextViewKeyListener());
        mQueryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());

        mCorpusIndicator.setOnClickListener(new CorpusIndicatorClickListener());

        mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());

        mVoiceSearchButton.setOnClickListener(new VoiceSearchButtonClickListener());

        ButtonsKeyListener buttonsKeyListener = new ButtonsKeyListener();
        mSearchGoButton.setOnKeyListener(buttonsKeyListener);
        mVoiceSearchButton.setOnKeyListener(buttonsKeyListener);
        mCorpusIndicator.setOnKeyListener(buttonsKeyListener);

        mUpdateSuggestions = true;

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSuggestionsView.setAdapter(mSuggestionsAdapter);
        mSuggestionsFooter.setAdapter(mSuggestionsAdapter);
    }

    private void startMethodTracing() {
        File traceDir = getDir("traces", 0);
        String traceFile = new File(traceDir, "qsb.trace").getAbsolutePath();
        Debug.startMethodTracing(traceFile);
    }

    @Override
    protected void onNewIntent(Intent intent) {
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
        String query = savedInstanceState.getString(INSTANCE_KEY_USER_QUERY);
        setCorpus(getCorpus(corpusName));
        setUserQuery(query);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.

        String corpusName = mCorpus == null ? null : mCorpus.getName();
        outState.putString(INSTANCE_KEY_CORPUS, corpusName);
        outState.putString(INSTANCE_KEY_USER_QUERY, mUserQuery);
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        Corpus corpus = getCorpusFromUri(intent.getData());
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);

        setCorpus(corpus);
        setUserQuery(query);
        mSelectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);
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

    private Corpus getCorpusFromUri(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_CORPUS.equals(uri.getScheme())) return null;
        String name = uri.getAuthority();
        return getCorpus(name);
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

    private void setCorpus(Corpus corpus) {
        if (DBG) Log.d(TAG, "setCorpus(" + corpus + ")");
        mCorpus = corpus;
        Drawable sourceIcon;
        if (corpus == null) {
            sourceIcon = getCorpusViewFactory().getGlobalSearchIcon();
        } else {
            sourceIcon = corpus.getCorpusIcon();
        }
        mSuggestionsAdapter.setCorpus(corpus);
        mCorpusIndicator.setImageDrawable(sourceIcon);

        updateUi(getQuery().length() == 0);
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private CorpusViewFactory getCorpusViewFactory() {
        return getQsbApplication().getCorpusViewFactory();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");
        super.onDestroy();
        mSuggestionsFooter.setAdapter(null);
        mSuggestionsView.setAdapter(null);  // closes mSuggestionsAdapter
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening he search
            // settings, or clicking a notification in the status bar.
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSuggestionsAdapter.setSuggestions(null);
        getQsbApplication().getShortcutRefresher().reset();
        dismissCorpusSelectionDialog();
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        setQuery(mUserQuery, mSelectAll);
        // Only select everything the first time after creating the activity.
        mSelectAll = false;
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

    /**
     * Sets the query as typed by the user. Does not update the suggestions
     * or the text in the query box.
     */
    protected void setUserQuery(String userQuery) {
        if (userQuery == null) userQuery = "";
        mUserQuery = userQuery;
    }

    protected String getQuery() {
        CharSequence q = mQueryTextView.getText();
        return q == null ? "" : q.toString();
    }

    /** 
     * Restores the query entered by the user.
     */
    private void restoreUserQuery() {
        if (DBG) Log.d(TAG, "Restoring query to '" + mUserQuery + "'");
        setQuery(mUserQuery, false);
    }

    /**
     * Sets the text in the query box. Does not update the suggestions,
     * and does not change the saved user-entered query.
     * {@link #restoreUserQuery()} will restore the query to the last
     * user-entered query.
     */
    private void setQuery(String query, boolean selectAll) {
        mUpdateSuggestions = false;
        mQueryTextView.setText(query);
        setTextSelection(selectAll);
        mUpdateSuggestions = true;
    }

    /**
     * Sets the text selection in the query text view.
     *
     * @param selectAll If {@code true}, selects the entire query.
     *        If {@false}, no characters are selected, and the cursor is placed
     *        at the end of the query.
     */
    private void setTextSelection(boolean selectAll) {
        if (selectAll) {
            mQueryTextView.selectAll();
        } else {
            mQueryTextView.setSelection(mQueryTextView.length());
        }
    }

    private void updateUi(boolean queryEmpty) {
        updateQueryTextView(queryEmpty);
        updateSearchGoButton(queryEmpty);
        updateVoiceSearchButton(queryEmpty);
    }

    private void updateQueryTextView(boolean queryEmpty) {
        if (queryEmpty) {
            if (mCorpus == null || mCorpus.isWebCorpus()) {
                mQueryTextView.setBackgroundResource(R.drawable.textfield_search_empty_google);
                mQueryTextView.setHint(null);
            } else {
                mQueryTextView.setBackgroundResource(R.drawable.textfield_search_empty);
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
        if (queryEmpty && mLauncher.shouldShowVoiceSearch(mCorpus)) {
            mVoiceSearchButton.setVisibility(View.VISIBLE);
            mQueryTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        } else {
            mVoiceSearchButton.setVisibility(View.GONE);
            mQueryTextView.setPrivateImeOptions(null);
        }
    }

    protected void showCorpusSelectionDialog() {
        if (mCorpusSelectionDialog == null) {
            mCorpusSelectionDialog = new CorpusSelectionDialog(this);
            mCorpusSelectionDialog.setOwnerActivity(this);
            mCorpusSelectionDialog.setOnDismissListener(new CorpusSelectorDismissListener());
            mCorpusSelectionDialog.setOnCorpusSelectedListener(new CorpusSelectionListener());
        }
        mCorpusSelectionDialog.show(mCorpus);
    }

    protected boolean isCorpusSelectionDialogShowing() {
        return mCorpusSelectionDialog != null && mCorpusSelectionDialog.isShowing();
    }

    protected void dismissCorpusSelectionDialog() {
        if (mCorpusSelectionDialog != null) {
            mCorpusSelectionDialog.dismiss();
        }
    }

    protected void onSearchClicked(int method) {
        String query = getQuery();
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) == 0) return;

        Corpus searchCorpus = mLauncher.getSearchCorpus(getCorpora(), mCorpus);
        if (searchCorpus == null) return;

        mTookAction = true;

        // Log search start
        getLogger().logSearch(mCorpus, method, query.length());

        // Create shortcut
        SuggestionData searchShortcut = searchCorpus.createSearchShortcut(query);
        if (searchShortcut != null) {
            DataSuggestionCursor cursor = new DataSuggestionCursor(query);
            cursor.add(searchShortcut);
            getShortcutRepository().reportClick(cursor, 0);
        }

        // Start search
        Intent intent = searchCorpus.createSearchIntent(query, mAppSearchData);
        mLauncher.launchIntent(intent);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        Corpus searchCorpus = mLauncher.getSearchCorpus(getCorpora(), mCorpus);
        if (searchCorpus == null) return;

        mTookAction = true;

        // Log voice search start
        getLogger().logVoiceSearch(searchCorpus);

        // Start voice search
        Intent intent = searchCorpus.createVoiceSearchIntent(mAppSearchData);
        mLauncher.launchIntent(intent);
    }

    protected SuggestionCursor getCurrentSuggestions() {
        return mSuggestionsAdapter.getCurrentSuggestions();
    }

    protected boolean launchSuggestion(int position) {
        SuggestionCursor suggestions = getCurrentSuggestions();
        if (position < 0 || position >= suggestions.getCount()) {
            Log.w(TAG, "Tried to launch invalid suggestion " + position);
            return false;
        }

        if (DBG) Log.d(TAG, "Launching suggestion " + position);
        mTookAction = true;

        // Log suggestion click
        Collection<Corpus> corpora = mSuggestionsAdapter.getSuggestions().getIncludedCorpora();
        getLogger().logSuggestionClick(position, suggestions, corpora);

        // Create shortcut
        getShortcutRepository().reportClick(suggestions, position);

        // Launch intent
        Intent intent = mLauncher.getSuggestionIntent(suggestions, position, mAppSearchData);
        mLauncher.launchIntent(intent);

        return true;
    }

    protected boolean onSuggestionLongClicked(int position) {
        if (DBG) Log.d(TAG, "Long clicked on suggestion " + position);
        return false;
    }

    protected boolean onSuggestionKeyDown(int position, int keyCode, KeyEvent event) {
        // Treat enter or search as a click
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH) {
            return launchSuggestion(position);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position == 0) {
            // Moved up from the top suggestion, restore the user query and focus query box
            if (DBG) Log.d(TAG, "Up and out");
            restoreUserQuery();
            return false;  // let the framework handle the move
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Moved left / right from a suggestion, keep current query, move
            // focus to query box, and move cursor to far left / right
            if (DBG) Log.d(TAG, "Left/right on a suggestion");
            String query = getQuery();
            int cursorPos = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : query.length();
            mQueryTextView.setSelection(cursorPos);
            mQueryTextView.requestFocus();
            updateSuggestions(query);
            return true;
        }

        return false;
    }

    protected void onSourceSelected() {
        if (DBG) Log.d(TAG, "No suggestion selected");
        restoreUserQuery();
    }

    protected int getSelectedPosition() {
        return mSuggestionsView.getSelectedPosition();
    }

    /**
     * Hides the input method.
     */
    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mQueryTextView.getWindowToken(), 0);
        }
    }

    protected void showInputMethodForQuery() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mQueryTextView, 0);
        }
    }

    /**
     * Hides the input method when the suggestions get focus.
     */
    private class SuggestListFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Suggestions focus change, now: " + focused);
            if (focused) {
                // The suggestions list got focus, hide the input method
                hideInputMethod();
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
        }
    }

    private List<Corpus> getCorporaToQuery() {
        if (mCorpus == null) {
            return getCorpusRanker().getRankedCorpora();
        } else {
            return Collections.singletonList(mCorpus);
        }
    }

    private int getMaxSuggestions() {
        Config config = getConfig();
        return mCorpus == null
                ? config.getMaxPromotedSuggestions()
                : config.getMaxResultsPerSource();
    }

    private void updateSuggestionsBuffered() {
        mHandler.removeCallbacks(mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        mHandler.postDelayed(mUpdateSuggestionsTask, delay);
    }

    private void updateSuggestions(String query) {
        // Log start latency if this is the first suggestions update
        if (mStarting) {
            mStarting = false;
            String source = getIntent().getStringExtra(Search.SOURCE);
            List<Corpus> rankedCorpora = getCorpusRanker().getRankedCorpora();
            int latency = mStartLatencyTracker.getLatency();
            getLogger().logStart(latency, source, mCorpus, rankedCorpora);
        }

        query = ltrim(query);
        List<Corpus> corporaToQuery = getCorporaToQuery();
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(
                query, corporaToQuery, getMaxSuggestions());
        mSuggestionsAdapter.setSuggestions(suggestions);
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
                String query = s == null ? "" : s.toString();
                setUserQuery(query);
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
                onSearchClicked(Logger.SEARCH_METHOD_KEYBOARD);
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
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int position = getSelectedPosition();
                if (onSuggestionKeyDown(position, keyCode, event)) {
                        return true;
                }
            }
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private class InputMethodCloser implements SuggestionsView.InteractionListener {
        public void onInteraction() {
            hideInputMethod();
        }
    }

    private class ClickHandler implements SuggestionClickListener {
       public void onSuggestionClicked(int position) {
           launchSuggestion(position);
       }

       public boolean onSuggestionLongClicked(int position) {
           return SearchActivity.this.onSuggestionLongClicked(position);
       }
    }

    private class SelectionHandler implements SuggestionSelectionListener {
        public void onSuggestionSelected(int position) {
            SuggestionCursor suggestions = getCurrentSuggestions();
            suggestions.moveTo(position);
            String displayQuery = suggestions.getSuggestionDisplayQuery();
            if (TextUtils.isEmpty(displayQuery)) {
                restoreUserQuery();
            } else {
                setQuery(displayQuery, false);
            }
        }

        public void onNothingSelected() {
                // This happens when a suggestion has been selected with the
                // dpad / trackball and then a different UI element is touched.
                // Do nothing, since we want to keep the query of the selection
                // in the search box.
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
        public void onCorpusSelected(Corpus corpus) {
            setCorpus(corpus);
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

    private static String ltrim(String text) {
        int start = 0;
        int length = text.length();
        while (start < length && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start > 0 ? text.substring(start, length) : text;
    }

}
