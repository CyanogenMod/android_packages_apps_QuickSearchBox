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

import com.android.quicksearchbox.util.MockNamedTaskExecutor;

import android.os.Handler;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Tests for {@link BlendingSuggestionsProvider}.
 */
@MediumTest
public class BlendingSuggestionsProviderTest extends AndroidTestCase {

    private MockCorpora mCorpora;
    private MockNamedTaskExecutor mTaskExecutor;
    private BlendingSuggestionsProvider mProvider;
    private ShortcutRepository mShortcutRepo;

    @Override
    protected void setUp() throws Exception {
        Config config = new Config(getContext());
        mTaskExecutor = new MockNamedTaskExecutor();
        Handler publishThread = new MockHandler();
        mShortcutRepo = new MockShortcutRepository();
        mCorpora = new MockCorpora();
        mCorpora.addCorpus(MockCorpus.CORPUS_1);
        mCorpora.addCorpus(MockCorpus.CORPUS_2);
        CorpusRanker corpusRanker = new LexicographicalCorpusRanker(mCorpora);
        Logger logger = new NoLogger();
        mProvider = new BlendingSuggestionsProvider(config,
                mTaskExecutor,
                publishThread,
                corpusRanker,
                logger);
        mProvider.setAllPromoter(new ConcatPromoter<CorpusResult>());
        mProvider.setSingleCorpusPromoter(new ConcatPromoter<CorpusResult>());
    }

    public void testSingleCorpus() {
        BlendedSuggestions suggestions = (BlendedSuggestions) mProvider.getSuggestions(
                "foo", MockCorpus.CORPUS_1, 3);
        suggestions.setShortcuts(mShortcutRepo.getShortcutsForQuery(
                "foo", mCorpora.getAllCorpora()));
        try {
            assertEquals(1, suggestions.getExpectedResultCount());
            assertEquals(0, suggestions.getResultCount());
            assertEquals(0, suggestions.getPromoted().getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(1, suggestions.getExpectedResultCount());
            assertEquals(1, suggestions.getResultCount());
            assertEquals(MockCorpus.CORPUS_1.getSuggestions("foo", 3, true).getCount(),
                    suggestions.getPromoted().getCount());
            mTaskExecutor.assertDone();
        } finally {
            if (suggestions != null) suggestions.close();
        }
    }

    public void testMultipleCorpora() {
        BlendedSuggestions suggestions = (BlendedSuggestions) mProvider.getSuggestions(
                "foo", null, 6);
        suggestions.setShortcuts(mShortcutRepo.getShortcutsForQuery(
                        "foo", mCorpora.getAllCorpora()));
        try {
            int corpus1Count = MockCorpus.CORPUS_1.getSuggestions("foo", 3, true).getCount();
            int corpus2Count = MockCorpus.CORPUS_2.getSuggestions("foo", 3, true).getCount();
            assertEquals(mCorpora.getEnabledCorpora().size(), suggestions.getExpectedResultCount());
            assertEquals(0, suggestions.getResultCount());
            assertEquals(0, suggestions.getPromoted().getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(1, suggestions.getResultCount());
            assertEquals("Incorrect promoted: " + suggestions.getPromoted(),
                    corpus1Count, suggestions.getPromoted().getCount());
            assertTrue(mTaskExecutor.runNext());
            assertEquals(2, suggestions.getResultCount());
            assertEquals("Incorrect promoted: " + suggestions.getPromoted(),
                    corpus1Count + corpus2Count, suggestions.getPromoted().getCount());
            mTaskExecutor.assertDone();
        } finally {
            if (suggestions != null) suggestions.close();
        }
    }

}
