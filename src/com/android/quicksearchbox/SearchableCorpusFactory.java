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

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Creates corpora.
 */
public class SearchableCorpusFactory implements CorpusFactory {

    private final Context mContext;

    public SearchableCorpusFactory(Context context) {
        mContext = context;
    }

    public Collection<Corpus> createCorpora(Sources sources) {
        Collection<Source> sourceList = sources.getSources();
        ArrayList<Corpus> corpora = new ArrayList<Corpus>(sourceList.size());

        Source webSource = sources.getWebSearchSource();
        Source browserSource = sources.getSource(getBrowserSearchComponent());
        Corpus webCorpus = createWebCorpus(webSource, browserSource);
        corpora.add(webCorpus);

        // Creates corpora for all unclaimed sources
        for (Source source : sourceList) {
            if (source != webSource && source != browserSource) {
                corpora.add(new SingleSourceCorpus(source));
            }
        }

        return corpora;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Corpus createWebCorpus(Source webSource, Source browserSource) {
        return new WebCorpus(mContext, webSource, browserSource);
    }

    private ComponentName getBrowserSearchComponent() {
        String name = mContext.getString(R.string.browser_search_component);
        return TextUtils.isEmpty(name) ? null : ComponentName.unflattenFromString(name);
    }

}
