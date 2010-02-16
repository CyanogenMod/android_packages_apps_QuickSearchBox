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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class DefaultCorpusRanker implements CorpusRanker {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.DefaultCorpusRanker";

    private final ShortcutRepository mShortcuts;

    public DefaultCorpusRanker(ShortcutRepository shortcuts) {
        mShortcuts = shortcuts;
    }

    private static class CorpusComparator implements Comparator<Corpus> {
        private final Map<String,Integer> mClickScores;

        public CorpusComparator(Map<String,Integer> clickScores) {
            mClickScores = clickScores;
        }

        public int compare(Corpus corpus1, Corpus corpus2) {
            // Note that this is reversed from the usual order
            return getCorpusScore(corpus2) - getCorpusScore(corpus1);
        }

        /**
         * Scores a corpus. Higher score is better.
         */
        private int getCorpusScore(Corpus corpus) {
            if (corpus.isWebCorpus()) {
                return Integer.MAX_VALUE;
            }
            Integer clickScore = mClickScores.get(corpus.getName());
            if (clickScore != null) {
                return clickScore;
            }
            return 0;
        }
    }

    public ArrayList<Corpus> rankCorpora(Collection<Corpus> corpora) {
        if (DBG) Log.d(TAG, "Ranking: " + corpora);

        Map<String,Integer> clickScores = mShortcuts.getCorpusScores();
        ArrayList<Corpus> ordered = new ArrayList<Corpus>(corpora);
        Collections.sort(ordered, new CorpusComparator(clickScores));

        if (DBG) Log.d(TAG, "Ordered: " + ordered);
        return ordered;
    }

}
