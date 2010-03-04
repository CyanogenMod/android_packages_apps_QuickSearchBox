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

import com.android.quicksearchbox.util.Util;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for RankAwarePromoter
 *
 */
public class RankAwarePromoterTest extends AndroidTestCase {
    public static final int MAX_PROMOTED_CORPORA = 3;
    public static final int MAX_PROMOTED_SUGGESTIONS = 8;
    public static final String TEST_QUERY = "query";

    private MockCorpora mCorpora;
    private CorpusRanker mRanker;
    private RankAwarePromoter mPromoter;

    @Override
    public void setUp() {
        mCorpora = createMockCorpora(5);
        mRanker = new LexicographicalCorpusRanker();
        mPromoter = new RankAwarePromoter();
    }

    public void testPromotesExpectedSuggestions() {
        ArrayList<CorpusResult> suggestions = getSuggestions(TEST_QUERY);
        ListSuggestionCursor promoted = new ListSuggestionCursor(TEST_QUERY);
        ArrayList<Corpus> rankedCorpora = getRankedCorpora();
        Set<Corpus> promotedCorpora = Util.setOfFirstN(rankedCorpora, MAX_PROMOTED_CORPORA);
        mPromoter.pickPromoted(null, suggestions, MAX_PROMOTED_SUGGESTIONS, promoted,
                promotedCorpora);

        assertEquals(MAX_PROMOTED_SUGGESTIONS, promoted.getCount());

        int[] expectedSource = {0, 0, 1, 1, 2, 2, 3, 4};
        int[] expectedSuggestion = {1, 2, 1, 2, 1, 2, 1, 1};

        for (int i = 0; i < promoted.getCount(); i++) {
            promoted.moveTo(i);
            assertEquals("Source in position " + i,
                    "MockSource Source" + expectedSource[i],
                    promoted.getSuggestionSource().getLabel());
            assertEquals("Suggestion in position " + i,
                    TEST_QUERY + "_" + expectedSuggestion[i],
                    promoted.getSuggestionText1());
        }
    }

    private ArrayList<Corpus> getRankedCorpora() {
        return mRanker.rankCorpora(mCorpora.getAllCorpora());
    }

    private ArrayList<CorpusResult> getSuggestions(String query) {
        ArrayList<CorpusResult> suggestions = new ArrayList<CorpusResult>();
        for (Corpus corpus : getRankedCorpora()) {
            suggestions.add(corpus.getSuggestions(query, 10));
        }
        return suggestions;
    }

    private static MockCorpora createMockCorpora(int count) {
        MockCorpora corpora = new MockCorpora();
        for (int i = 0; i < count; i++) {
            Source mockSource = new MockSource("Source" + i);
            corpora.addCorpus(new MockCorpus(mockSource), mockSource);
        }
        return corpora;
    }

    // A corpus ranker that orders corpora lexicographically by name.
    private static class LexicographicalCorpusRanker implements CorpusRanker {
        public ArrayList<Corpus> rankCorpora(Collection<Corpus> corpora) {
            ArrayList<Corpus> ordered = new ArrayList<Corpus>(corpora);
            Collections.sort(ordered, new Comparator<Corpus>() {
                public int compare(Corpus c1, Corpus c2) {
                    return c1.getName().compareTo(c2.getName());
                }
            });
            return ordered;
        }
    }
}
