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

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 * Mock implementation of {@link Corpora}.
 */
public class MockCorpora implements Corpora {

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private HashMap<String,Corpus> mCorporaByName = new HashMap<String,Corpus>();
    private HashMap<Source,Corpus> mCorporaBySource = new HashMap<Source,Corpus>();
    private HashMap<String,Source> mSourcesByName = new HashMap<String,Source>();
    private Corpus mWebCorpus;

    public void addCorpus(Corpus corpus, Source... sources) {
        mCorporaByName.put(corpus.getName(), corpus);
        for (Source source : sources) {
            mCorporaBySource.put(source, corpus);
            mSourcesByName.put(source.getName(), source);
        }
        notifyDataSetChanged();
    }

    public void setWebCorpus(Corpus webCorpus) {
        mWebCorpus = webCorpus;
    }

    public Collection<Corpus> getAllCorpora() {
        return Collections.unmodifiableCollection(mCorporaByName.values());
    }

    public Corpus getCorpus(String name) {
        return mCorporaByName.get(name);
    }

    public Corpus getWebCorpus() {
        return mWebCorpus;
    }

    public Corpus getCorpusForSource(Source source) {
        return mCorporaBySource.get(source);
    }

    public Collection<Corpus> getEnabledCorpora() {
        return getAllCorpora();
    }

    public Source getSource(String name) {
        return mSourcesByName.get(name);
    }

    public boolean isCorpusDefaultEnabled(Corpus corpus) {
        return true;
    }

    public boolean isCorpusEnabled(Corpus corpus) {
        return true;
    }

    public void close() {
        // Nothing to release
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

}
