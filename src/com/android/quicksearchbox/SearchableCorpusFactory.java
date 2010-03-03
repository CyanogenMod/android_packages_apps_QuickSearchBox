/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Creates corpora.
 */
public class SearchableCorpusFactory implements CorpusFactory {

    private final Context mContext;

    private final NamedTaskExecutor mExecutor;

    public SearchableCorpusFactory(Context context, NamedTaskExecutor executor) {
        mContext = context;
        mExecutor = executor;
    }

    public Collection<Corpus> createCorpora(Sources sources) {
        Collection<Source> sourceList = sources.getSources();
        ArrayList<Corpus> corpora = new ArrayList<Corpus>(sourceList.size());
        HashSet<Source> claimedSources = new HashSet<Source>();

        Source webSource = sources.getWebSearchSource();
        Source browserSource = sources.getSource(getBrowserSearchComponent());
        Corpus webCorpus = createWebCorpus(webSource, browserSource);
        claimedSources.add(webSource);
        claimedSources.add(browserSource);
        corpora.add(webCorpus);

        Source appsSource = sources.getSource(getAppsSearchComponent());
        Corpus appsCorpus = createAppsCorpus(appsSource);
        claimedSources.add(appsSource);
        corpora.add(appsCorpus);

        // Creates corpora for all unclaimed sources
        for (Source source : sourceList) {
            if (!claimedSources.contains(source)) {
                corpora.add(new SingleSourceCorpus(source));
            }
        }

        return corpora;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Corpus createWebCorpus(Source webSource, Source browserSource) {
        return new WebCorpus(mContext, mExecutor, webSource, browserSource);
    }

    protected Corpus createAppsCorpus(Source appsSource) {
        return new AppsCorpus(mContext, mExecutor, appsSource);
    }

    private ComponentName getBrowserSearchComponent() {
        return getComponentNameResource(R.string.browser_search_component);
    }

    private ComponentName getAppsSearchComponent() {
        return getComponentNameResource(R.string.installed_apps_component);
    }

    private ComponentName getComponentNameResource(int res) {
        String name = mContext.getString(res);
        return TextUtils.isEmpty(name) ? null : ComponentName.unflattenFromString(name);
    }

}
