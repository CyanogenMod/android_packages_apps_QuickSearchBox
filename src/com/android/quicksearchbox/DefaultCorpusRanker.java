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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.PriorityQueue;

public class DefaultCorpusRanker implements CorpusRanker {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.DefaultCorpusRanker";

    private final ShortcutRepository mShortcuts;

    public DefaultCorpusRanker(ShortcutRepository shortcuts) {
        mShortcuts = shortcuts;
    }

    private static class ScoredCorpus implements Comparable<ScoredCorpus> {
        public final Corpus mCorpus;
        public final int mScore;
        public ScoredCorpus(Corpus corpus, int score) {
            mCorpus = corpus;
            mScore = score;
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof ScoredCorpus)) return false;
            ScoredCorpus sc = (ScoredCorpus) o;
            return mCorpus.equals(sc.mCorpus) && mScore == sc.mScore;
        }

        public int compareTo(ScoredCorpus another) {
            int scoreDiff = mScore - another.mScore;
            if (scoreDiff != 0) {
                return scoreDiff;
            } else {
                return mCorpus.getName().compareTo(another.mCorpus.getName());
            }
        }

        @Override
        public String toString() {
            return mCorpus + ":" + mScore;
        }
    }

    /**
     * Scores a corpus. Higher score is better.
     */
    private int getCorpusScore(Corpus corpus, Map<String,Integer> clickScores) {
        if (corpus.isWebCorpus()) {
            return Integer.MAX_VALUE;
        }
        Integer clickScore = clickScores.get(clickScores);
        if (clickScore != null) {
            return clickScore;
        }
        return 0;
    }

    public ArrayList<Corpus> rankCorpora(Collection<Corpus> corpora) {
        if (DBG) Log.d(TAG, "Ranking: " + corpora);
        // For some reason, PriorityQueue throws IllegalArgumentException if given
        // an initial capacity < 1.
        int capacity = 1 + corpora.size();
        // PriorityQueue returns smallest element first, so we need a reverse comparator
        PriorityQueue<ScoredCorpus> queue =
                new PriorityQueue<ScoredCorpus>(capacity,
                        new ReverseComparator<ScoredCorpus>());
        Map<String,Integer> clickScores = mShortcuts.getCorpusScores();
        for (Corpus corpus : corpora) {
            int score = getCorpusScore(corpus, clickScores);
            queue.add(new ScoredCorpus(corpus, score));
        }
        if (DBG) Log.d(TAG, "Corpus scores: " + queue);
        ArrayList<Corpus> ordered = new ArrayList<Corpus>(queue.size());
        for (ScoredCorpus scoredCorpus : queue) {
            ordered.add(scoredCorpus.mCorpus);
        }
        return ordered;
    }

}
