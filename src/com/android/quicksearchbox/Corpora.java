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

import android.content.ComponentName;

import java.util.Collection;

/**
 * Maintains the set of available and enabled corpora.
 */
public interface Corpora {

    boolean isCorpusEnabled(Corpus corpus);

    /**
     * Checks if a corpus should be enabled by default.
     */
    boolean isCorpusDefaultEnabled(Corpus corpus);

    /**
     * Gets all corpora, including the web corpus.
     *
     * @return Callers must not modify the returned collection.
     */
    Collection<Corpus> getAllCorpora();

    Collection<Corpus> getEnabledCorpora();

    /**
     * Gets a corpus by name.
     *
     * @return A corpus, or null.
     */
    Corpus getCorpus(String name);

    Source getSource(ComponentName name);

    /**
     * Gets the corpus that contains the given source.
     */
    Corpus getCorpusForSource(Source source);

    void close();
}
