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

import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewInflater;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ThreadFactory;

public class QsbApplication extends Application {

    private Config mConfig;
    private Sources mSources;
    private ShortcutRepository mShortcutRepository;
    private SourceTaskExecutor mSourceTaskExecutor;
    private SuggestionsProvider mSuggestionsProvider;
    private SuggestionViewFactory mSuggestionViewFactory;

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
        if (mSuggestionsProvider != null) {
            mSuggestionsProvider.close();
            mSuggestionsProvider = null;
        }
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
        Sources sources = new Sources(this, getConfig());
        sources.load();
        return sources;
    }

    public ShortcutRepository getShortcutRepository() {
        if (mShortcutRepository == null) {
            mShortcutRepository = createShortcutRepository();
        }
        return mShortcutRepository;
    }

    protected ShortcutRepository createShortcutRepository() {
        return ShortcutRepositoryImplLog.create(this, getConfig(), getSources());
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

    public SuggestionsProvider getSuggestionsProvider() {
        if (mSuggestionsProvider == null) {
            mSuggestionsProvider = createSuggestionsProvider();
        }
        return mSuggestionsProvider;
    }

    protected SuggestionsProvider createSuggestionsProvider() {
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

}
