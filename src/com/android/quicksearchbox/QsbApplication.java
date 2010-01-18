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

import com.android.quicksearchbox.ui.DelayingSuggestionsAdapter;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewInflater;
import com.android.quicksearchbox.ui.SuggestionsAdapter;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ThreadFactory;

public class QsbApplication extends Application {

    private static final String TAG ="QSB.QsbApplication";

    private Handler mUiThreadHandler;
    private Config mConfig;
    private Sources mSources;
    private ShortcutRepository mShortcutRepository;
    private ShortcutRefresher mShortcutRefresher;
    private SourceTaskExecutor mSourceTaskExecutor;
    private SuggestionsProvider mGlobalSuggestionsProvider;
    private SuggestionViewFactory mSuggestionViewFactory;
    private SourceFactory mSourceFactory;

    @Override
    public void onTerminate() {
        close();
        super.onTerminate();
    }

    protected void close() {
        if (mSources != null) {
            mSources.close();
            mSources = null;
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

    public Handler getUiThreadHandler() {
        if (mUiThreadHandler == null) {
            mUiThreadHandler = createUiThreadHandler();
        }
        return mUiThreadHandler;
    }

    protected Handler createUiThreadHandler() {
        return new Handler(Looper.myLooper());
    }

    public Config getConfig() {
        if (mConfig == null) {
            mConfig = createConfig();
        }
        return mConfig;
    }

    protected Config createConfig() {
        return new Config(this);
    }

    public SourceLookup getSources() {
        if (mSources == null) {
            mSources = createSources();
        }
        return mSources;
    }

    protected Sources createSources() {
        Sources sources = new Sources(this, getConfig(), getSourceFactory());
        sources.load();
        return sources;
    }

    public ShortcutRepository getShortcutRepository() {
        if (mShortcutRepository == null) {
            mShortcutRepository = createShortcutRepository();
        }
        return mShortcutRepository;
    }

    public ShortcutRefresher getShortcutRefresher() {
        if (mShortcutRefresher == null) {
            mShortcutRefresher = createShortcutRefresher();
        }
        return mShortcutRefresher;
    }

    protected ShortcutRefresher createShortcutRefresher() {
        // For now, ShortcutRefresher gets its own SourceTaskExecutor
        return new ShortcutRefresher(createSourceTaskExecutor(), getSources());
    }

    protected ShortcutRepository createShortcutRepository() {
        return ShortcutRepositoryImplLog.create(this, getConfig(), getSources(),
            getShortcutRefresher(), getUiThreadHandler());
    }

    public SourceTaskExecutor getSourceTaskExecutor() {
        if (mSourceTaskExecutor == null) {
            mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return mSourceTaskExecutor;
    }

    protected SourceTaskExecutor createSourceTaskExecutor() {
        Config config = getConfig();
        ThreadFactory queryThreadFactory =
            new QueryThreadFactory(config.getQueryThreadPriority());
        return new DelayingSourceTaskExecutor(config, queryThreadFactory);
    }


    public SuggestionsProvider getSuggestionsProvider(Source source) {
        if (source == null) {
            return getGlobalSuggestionsProvider();
        }
        // TODO: Cache this to avoid creating a new one for each key press
        return createSuggestionsProvider(source);
    }

    protected SuggestionsProvider createSuggestionsProvider(Source source) {
        // TODO: We could use simpler promoter here
        Promoter promoter =  new ShortcutPromoter(new RoundRobinPromoter());
        SingleSourceSuggestionsProvider provider = new SingleSourceSuggestionsProvider(getConfig(),
                source,
                getSourceTaskExecutor(),
                getUiThreadHandler(),
                promoter,
                getShortcutRepository());
        return provider;
    }

    public SuggestionsProvider getGlobalSuggestionsProvider() {
        if (mGlobalSuggestionsProvider == null) {
            mGlobalSuggestionsProvider = createGlobalSuggestionsProvider();
        }
        return mGlobalSuggestionsProvider;
    }

    protected SuggestionsProvider createGlobalSuggestionsProvider() {
        Handler uiThread = new Handler(Looper.myLooper());
        Promoter promoter =  new ShortcutPromoter(new RoundRobinPromoter());
        GlobalSuggestionsProvider provider = new GlobalSuggestionsProvider(getConfig(),
                getSources(),
                getSourceTaskExecutor(),
                uiThread,
                promoter,
                getShortcutRepository());
        return provider;
    }

    public SuggestionViewFactory getSuggestionViewFactory() {
        if (mSuggestionViewFactory == null) {
            mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return mSuggestionViewFactory;
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new SuggestionViewInflater(this);
    }

    public SuggestionsAdapter createSuggestionsAdapter() {
        Config config = getConfig();
        SuggestionViewFactory viewFactory = getSuggestionViewFactory();
        DelayingSuggestionsAdapter adapter = new DelayingSuggestionsAdapter(viewFactory);
        return adapter;
    }

    public SourceFactory getSourceFactory() {
        if (mSourceFactory == null) {
            mSourceFactory = createSourceFactory();
        }
        return mSourceFactory;
    }

    protected SourceFactory createSourceFactory() {
        return new SearchableSourceFactory(this);
    }
}
