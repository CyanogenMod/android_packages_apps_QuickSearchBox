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

import com.android.quicksearchbox.ui.CorpusViewFactory;
import com.android.quicksearchbox.ui.CorpusViewInflater;
import com.android.quicksearchbox.ui.DelayingSuggestionsAdapter;
import com.android.quicksearchbox.ui.EmptySuggestionsFooter;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewInflater;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.ui.SuggestionsFooter;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PerNameExecutor;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ThreadFactory;

public class QsbApplication extends Application {

    private Handler mUiThreadHandler;
    private Config mConfig;
    private Corpora mCorpora;
    private CorpusRanker mCorpusRanker;
    private ShortcutRepository mShortcutRepository;
    private ShortcutRefresher mShortcutRefresher;
    private NamedTaskExecutor mSourceTaskExecutor;
    private SuggestionsProvider mGlobalSuggestionsProvider;
    private SuggestionViewFactory mSuggestionViewFactory;
    private CorpusViewFactory mCorpusViewFactory;
    private Logger mLogger;

    @Override
    public void onTerminate() {
        close();
        super.onTerminate();
    }

    protected void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Accessed Application object from thread "
                    + Thread.currentThread().getName());
        }
    }

    protected void close() {
        checkThread();
        if (mConfig != null) {
            mConfig.close();
            mConfig = null;
        }
        if (mCorpora != null) {
            mCorpora.close();
            mCorpora = null;
        }
        if (mShortcutRepository != null) {
            mShortcutRepository.close();
            mShortcutRepository = null;
        }
        if (mSourceTaskExecutor != null) {
            mSourceTaskExecutor.close();
            mSourceTaskExecutor = null;
        }
        if (mGlobalSuggestionsProvider != null) {
            mGlobalSuggestionsProvider.close();
            mGlobalSuggestionsProvider = null;
        }
    }

    public synchronized Handler getMainThreadHandler() {
        if (mUiThreadHandler == null) {
            mUiThreadHandler = new Handler(Looper.getMainLooper());
        }
        return mUiThreadHandler;
    }

    /**
     * Gets the QSB configuration object.
     * May be called from any thread.
     */
    public synchronized Config getConfig() {
        if (mConfig == null) {
            mConfig = createConfig();
        }
        return mConfig;
    }

    protected Config createConfig() {
        return new Config(this);
    }

    /**
     * Gets the corpora.
     * May only be called from the main thread.
     */
    public Corpora getCorpora() {
        checkThread();
        if (mCorpora == null) {
            mCorpora = createCorpora();
        }
        return mCorpora;
    }

    protected Corpora createCorpora() {
        SearchableCorpora corpora = new SearchableCorpora(this, getConfig(), createSources(),
                createCorpusFactory());
        corpora.load();
        return corpora;
    }

    protected Sources createSources() {
        return new SearchableSources(this, getMainThreadHandler());
    }

    protected CorpusFactory createCorpusFactory() {
        return new SearchableCorpusFactory(this, getSourceTaskExecutor());
    }

    /**
     * Gets the corpus ranker.
     * May only be called from the main thread.
     */
    public CorpusRanker getCorpusRanker() {
        checkThread();
        if (mCorpusRanker == null) {
            mCorpusRanker = createCorpusRanker();
        }
        return mCorpusRanker;
    }

    protected CorpusRanker createCorpusRanker() {
        return new DefaultCorpusRanker(getCorpora(), getShortcutRepository());
    }

    /**
     * Gets the shortcut repository.
     * May only be called from the main thread.
     */
    public ShortcutRepository getShortcutRepository() {
        checkThread();
        if (mShortcutRepository == null) {
            mShortcutRepository = createShortcutRepository();
        }
        return mShortcutRepository;
    }

    protected ShortcutRepository createShortcutRepository() {
        return ShortcutRepositoryImplLog.create(this, getConfig(), getCorpora(),
            getShortcutRefresher(), getMainThreadHandler());
    }

    /**
     * Gets the shortcut refresher.
     * May only be called from the main thread.
     */
    public ShortcutRefresher getShortcutRefresher() {
        checkThread();
        if (mShortcutRefresher == null) {
            mShortcutRefresher = createShortcutRefresher();
        }
        return mShortcutRefresher;
    }

    protected ShortcutRefresher createShortcutRefresher() {
        // For now, ShortcutRefresher gets its own SourceTaskExecutor
        return new SourceShortcutRefresher(createSourceTaskExecutor());
    }

    /**
     * Gets the source task executor.
     * May only be called from the main thread.
     */
    public NamedTaskExecutor getSourceTaskExecutor() {
        checkThread();
        if (mSourceTaskExecutor == null) {
            mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return mSourceTaskExecutor;
    }

    protected NamedTaskExecutor createSourceTaskExecutor() {
        Config config = getConfig();
        ThreadFactory queryThreadFactory =
            new QueryThreadFactory(config.getQueryThreadPriority());
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(queryThreadFactory));
    }

    /**
     * Gets the suggestion provider for a corpus.
     * May only be called from the main thread.
     */
    public SuggestionsProvider getSuggestionsProvider(Corpus corpus) {
        checkThread();
        if (corpus == null) {
            return getGlobalSuggestionsProvider();
        }
        // TODO: Cache this to avoid creating a new one for each key press
        return createSuggestionsProvider(corpus);
    }

    protected SuggestionsProvider createSuggestionsProvider(Corpus corpus) {
        // TODO: We could use simpler promoter here
        Promoter promoter =  new ShortcutPromoter(new RoundRobinPromoter());
        SingleCorpusSuggestionsProvider provider = new SingleCorpusSuggestionsProvider(getConfig(),
                corpus,
                getSourceTaskExecutor(),
                getMainThreadHandler(),
                promoter,
                getShortcutRepository());
        return provider;
    }

    protected SuggestionsProvider getGlobalSuggestionsProvider() {
        checkThread();
        if (mGlobalSuggestionsProvider == null) {
            mGlobalSuggestionsProvider = createGlobalSuggestionsProvider();
        }
        return mGlobalSuggestionsProvider;
    }

    protected SuggestionsProvider createGlobalSuggestionsProvider() {
        Promoter promoter =  new ShortcutPromoter(new RankAwarePromoter());
        GlobalSuggestionsProvider provider = new GlobalSuggestionsProvider(getConfig(),
                getSourceTaskExecutor(),
                getMainThreadHandler(),
                promoter,
                getCorpusRanker(),
                getShortcutRepository());
        return provider;
    }

    /**
     * Gets the suggestion view factory.
     * May only be called from the main thread.
     */
    public SuggestionViewFactory getSuggestionViewFactory() {
        checkThread();
        if (mSuggestionViewFactory == null) {
            mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return mSuggestionViewFactory;
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new SuggestionViewInflater(this);
    }

    /**
     * Gets the corpus view factory.
     * May only be called from the main thread.
     */
    public CorpusViewFactory getCorpusViewFactory() {
        checkThread();
        if (mCorpusViewFactory == null) {
            mCorpusViewFactory = createCorpusViewFactory();
        }
        return mCorpusViewFactory;
    }

    protected CorpusViewFactory createCorpusViewFactory() {
        return new CorpusViewInflater(this);
    }

    /**
     * Creates a suggestions adapter.
     * May only be called from the main thread.
     */
    public SuggestionsAdapter createSuggestionsAdapter() {
        Config config = getConfig();
        SuggestionViewFactory viewFactory = getSuggestionViewFactory();
        DelayingSuggestionsAdapter adapter = new DelayingSuggestionsAdapter(viewFactory);
        return adapter;
    }

    /**
     * Creates a footer view to add at the bottom of the search activity.
     */
    public SuggestionsFooter createSuggestionsFooter() {
        return new EmptySuggestionsFooter(this);
    }

    /**
     * Gets the event logger.
     * May only be called from the main thread.
     */
    public Logger getLogger() {
        checkThread();
        if (mLogger == null) {
            mLogger = createLogger();
        }
        return mLogger;
    }

    protected Logger createLogger() {
        return new EventLogLogger(this);
    }
}
