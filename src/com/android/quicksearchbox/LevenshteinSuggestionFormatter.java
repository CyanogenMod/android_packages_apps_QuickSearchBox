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
        final Token[] queryTokens = tokenize(query);
        final Token[] suggestionTokens = tokenize(suggestion);
        final int[] matches = findMatches(queryTokens, suggestionTokens);
        if (DBG){
            Log.d(TAG, "source = " + queryTokens);
            Log.d(TAG, "target = " + suggestionTokens);
            Log.d(TAG, "matches = " + matches);
        }
        final SpannableString str = new SpannableString(suggestion);

        final int matchesLen = matches.length;
        for (int i = 0; i < matchesLen; ++i) {
            final Token t = suggestionTokens[i];
            int sourceLen = 0;
            int thisMatch = matches[i];
            if (thisMatch >= 0) {
                sourceLen = queryTokens[thisMatch].length();
            }
            applySuggestedTextStyle(str, t.mStart + sourceLen, t.mEnd);
            applyQueryTextStyle(str, t.mStart, t.mStart + sourceLen);
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
    int[] findMatches(Token[] source, Token[] target) {
        final LevenshteinDistance<Token> table = new LevenshteinDistance<Token>(source, target) {
            @Override
            protected boolean match(final Token source, final Token target) {
                return source.prefixOf(target);
            }
        };
        table.calculate();
        final int targetLen = target.length;
        final int[] result = new int[targetLen];
        LevenshteinDistance.EditOperation[] ops = table.getTargetOperations();
        for (int i = 0; i < targetLen; ++i) {
            if (ops[i].getType() == LevenshteinDistance.EDIT_UNCHANGED) {
                result[i] = ops[i].getPosition();
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    @VisibleForTesting
    Token[] tokenize(final CharSequence seq) {
        int pos = 0;
        final int len = seq.length();
        final char[] chars = new char[len];
        seq.toString().getChars(0, len, chars, 0);
        ArrayList<Token> tokens = new ArrayList<Token>();
        while (pos < len) {
            while (pos < len && Character.isWhitespace((int) seq.charAt(pos))) {
                pos++;
            }
            int start = pos;
            while (pos < len && !Character.isWhitespace((int) seq.charAt(pos))) {
                pos++;
            }
            int end = pos;
            if (start != end) {
                Token t = new Token(chars, start, end);
                tokens.add(t);
            }
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    @VisibleForTesting
    static class Token implements CharSequence {
        private final char[] mContainer;
        public final int mStart;
        public final int mEnd;

        public Token(char[] container, int start, int end) {
            mContainer = container;
            mStart = start;
            mEnd = end;
        }

        public int length() {
            return mEnd - mStart;
        }

        @Override
        public String toString() {
            // used in tests only.
            return subSequence(0, length());
        }

        public boolean prefixOf(final Token that) {
            final int len = length();
            if (len > that.length()) return false;
            final int thisStart = mStart;
            final int thatStart = that.mStart;
            final char[] thisContainer = mContainer;
            final char[] thatContainer = that.mContainer;
            for (int i = 0; i < len; ++i) {
                if (thisContainer[thisStart + i] != thatContainer[thatStart + i]) {
                    return false;
                }
            }
            return true;
        }

        public char charAt(int index) {
            return mContainer[index + mStart];
        }

        public String subSequence(int start, int end) {
            return new String(mContainer, mStart + start, length());
        }

    }

}
