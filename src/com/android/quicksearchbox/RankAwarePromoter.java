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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * A promoter that gives preference to suggestions from higher ranking corpora.
 */
public class RankAwarePromoter implements Promoter {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.RankAwarePromoter";

    public void pickPromoted(SuggestionCursor shortcuts, ArrayList<CorpusResult> suggestions,
            int maxPromoted, ListSuggestionCursor promoted, Set<Corpus> promotedCorpora) {

        if (DBG) Log.d(TAG, "Available results: " + suggestions);

        // Split non-empty results into promoted and other, positioned at first suggestion
        LinkedList<CorpusResult> promotedResults = new LinkedList<CorpusResult>();
        LinkedList<CorpusResult> otherResults = new LinkedList<CorpusResult>();
        for (int i = 0; i < suggestions.size(); i++) {
            CorpusResult result = suggestions.get(i);
            if (result.getCount() > 0) {
                result.moveTo(0);
                if (promotedCorpora.contains(result.getCorpus())) {
                    promotedResults.add(result);
                } else {
                    otherResults.add(result);
                }
            }
        }

        // Pick 2 results from each of the promoted corpora
        maxPromoted -= roundRobin(promotedResults, maxPromoted, 2, promoted);

        // Then try to fill with the remaining promoted results
        if (maxPromoted > 0 && !promotedResults.isEmpty()) {
            int stripeSize = Math.max(1, maxPromoted / promotedResults.size());
            maxPromoted -= roundRobin(promotedResults, maxPromoted, stripeSize, promoted);
            // We may still have a few slots left
            maxPromoted -= roundRobin(promotedResults, maxPromoted, maxPromoted, promoted);
        }

        // Then try to fill with the rest
        if (maxPromoted > 0 && !otherResults.isEmpty()) {
            int stripeSize = Math.max(1, maxPromoted / otherResults.size());
            maxPromoted -= roundRobin(otherResults, maxPromoted, stripeSize, promoted);
            // We may still have a few slots left
            maxPromoted -= roundRobin(otherResults, maxPromoted, maxPromoted, promoted);
        }

        if (DBG) Log.d(TAG, "Returning " + promoted.toString());
    }

    /**
     * Promotes "stripes" of suggestions from each corpus.
     *
     * @param results     the list of CorpusResults from which to promote.
     *                    Exhausted CorpusResults are removed from the list.
     * @param maxPromoted maximum number of suggestions to promote.
     * @param stripeSize  number of suggestions to take from each corpus.
     * @param promoted    the list to which promoted suggestions are added.
     * @return the number of suggestions actually promoted.
     */
    private int roundRobin(LinkedList<CorpusResult> results, int maxPromoted, int stripeSize,
            ListSuggestionCursor promoted) {
        int count = 0;
        if (maxPromoted > 0 && !results.isEmpty()) {
            for (Iterator<CorpusResult> iter = results.iterator();
                 count < maxPromoted && iter.hasNext();) {
                CorpusResult result = iter.next();
                count += promote(result, stripeSize, promoted);
                if (result.getPosition() == result.getCount()) {
                    iter.remove();
                }
            }
        }
        return count;
    }

    /**
     * Copies suggestions from a SuggestionCursor to the list of promoted suggestions.
     *
     * @param cursor from which to copy the suggestions
     * @param count maximum number of suggestions to copy
     * @param promoted the list to which to add the suggestions
     * @return the number of suggestions actually copied.
     */
    private int promote(SuggestionCursor cursor, int count, ListSuggestionCursor promoted) {
        if (count < 1 || cursor.getPosition() >= cursor.getCount()) {
            return 0;
        }
        int i = 0;
        do {
            promoted.add(new SuggestionPosition(cursor));
            i++;
        } while (cursor.moveToNext() && i < count);
        return i;
    }
}
