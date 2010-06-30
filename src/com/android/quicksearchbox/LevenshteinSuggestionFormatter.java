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

import com.android.quicksearchbox.util.LevenshteinDistance;
import com.google.common.annotations.VisibleForTesting;

import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion formatter using the Levenshtein distance (minumum edit distance) to calculate the
 * formatting.
 */
public class LevenshteinSuggestionFormatter extends SuggestionFormatter {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.LevenshteinSuggestionFormatter";

    public LevenshteinSuggestionFormatter(TextAppearanceFactory spanFactory) {
        super(spanFactory);
    }

    @Override
    public Spanned formatSuggestion(CharSequence query, CharSequence suggestion) {
        if (DBG) Log.d(TAG, "formatSuggestion('" + query + "', '" + suggestion + "')");
        query = normalizeQuery(query);
        List<Token> queryTokens = tokenize(query);
        List<Token> suggestionTokens = tokenize(suggestion);
        int[] matches = findMatches(queryTokens, suggestionTokens);
        if (DBG){
            Log.d(TAG, "source = " + queryTokens);
            Log.d(TAG, "target = " + suggestionTokens);
            Log.d(TAG, "matches = " + matches);
        }
        SpannableString str = new SpannableString(suggestion);

        for (int i = 0; i < matches.length; ++i) {
            Token t = suggestionTokens.get(i);
            int sourceLen = 0;
            if (matches[i] >= 0) {
                sourceLen = queryTokens.get(matches[i]).getLength();
            }
            applySuggestedTextStyle(str, t.getStart() + sourceLen, t.getEnd());
            applyQueryTextStyle(str, t.getStart(), t.getStart() + sourceLen);
        }

        return str;
    }

    private CharSequence normalizeQuery(CharSequence query) {
        return query.toString().toLowerCase();
    }

    /**
     * Finds which tokens in the target match tokens in the source.
     *
     * @param source List of source tokens (i.e. user query)
     * @param target List of target tokens (i.e. suggestion)
     * @return The indices into source which target tokens correspond to. A non-negative value n at
     *      position i means that target token i matches source token n. A negative value means that
     *      the target token i does not match any source token.
     */
    @VisibleForTesting
    int[] findMatches(List<Token> source, List<Token> target) {
        LevenshteinDistance<Token> table = new LevenshteinDistance<Token>(source, target) {
            @Override
            protected boolean match(Token source, Token target) {
                return source.prefixOf(target);
            }
        };
        table.calculate();
        int[] result = new int[target.size()];
        LevenshteinDistance.EditOperation[] ops = table.getTargetOperations();
        for (int i = 0; i < result.length; ++i) {
            if (ops[i].getType() == LevenshteinDistance.EDIT_UNCHANGED) {
                result[i] = ops[i].getPosition();
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    @VisibleForTesting
    List<Token> tokenize(CharSequence seq) {
        int pos = 0;
        ArrayList<Token> tokens = new ArrayList<Token>();
        while (pos < seq.length()) {
            while (pos < seq.length() && Character.isWhitespace(seq.charAt(pos))) {
                pos++;
            }
            int start = pos;
            while (pos < seq.length() && !Character.isWhitespace(seq.charAt(pos))) {
                pos++;
            }
            int end = pos;
            if (start != end) {
                Token t = new Token(seq, start, end);
                tokens.add(t);
            }
        }
        return tokens;
    }

    @VisibleForTesting
    static class Token {
        private final CharSequence mContainer;
        private final int mStart;
        private final int mEnd;

        public Token(CharSequence container, int start, int end) {
            mContainer = container;
            mStart = start;
            mEnd = end;
        }

        public int getStart() {
            return mStart;
        }

        public int getEnd() {
            return mEnd;
        }

        public int getLength() {
            return mEnd - mStart;
        }

        @Override
        public String toString() {
            return getSequence().toString();
        }

        public CharSequence getSequence() {
            return mContainer.subSequence(mStart, mEnd);
        }

        public boolean prefixOf(Token that) {
            return that.toString().startsWith(toString());
        }

    }

}
