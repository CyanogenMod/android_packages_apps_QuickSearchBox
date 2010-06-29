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

package com.android.quicksearchbox.util;

import java.util.List;

/**
 * This class represents the matrix used in the Levenshtein distance algorithm, together
 * with the algorithm itself which operates on the matrix.
 *
 * We also track of the individual operations applied to transform the source string into the
 * target string so we can trace the path taken through the matrix afterwards, in order to
 * perform the formatting as required.
 */
public abstract class LevenshteinDistance<T> {
    public static final int EDIT_DELETE = 0;
    public static final int EDIT_INSERT = 1;
    public static final int EDIT_REPLACE = 2;
    public static final int EDIT_UNCHANGED = 3;

    private final List<T> mSource;
    private final List<T> mTarget;
    private final Entry[][] mTable;

    public LevenshteinDistance(List<T> source, List<T> target) {
        mTable = new Entry[source.size()+1][target.size()+1];
        mSource = source;
        mTarget = target;
        set(0, 0, EDIT_UNCHANGED, 0);
        for (int i = 1; i <= source.size(); ++i) {
            set(i, 0, EDIT_DELETE, i);
        }
        for (int i = 1; i <= target.size(); ++i) {
            set(0, i, EDIT_INSERT, i);
        }
    }

    /**
     * Compares a source and target token.
     * @param source Token from the source string.
     * @param target Token from the target string.
     * @return {@code true} if the two are the same for the purposes of edit distance calculation,
     *      {@code false} otherwise.
     */
    protected abstract boolean match(T source, T target);

    /**
     * Implementation of Levenshtein distance algorithm.
     *
     * @return The Levenshtein distance.
     */
    public int calculate() {
        for (int s = 1; s <= mSource.size(); ++s) {
            T sourceToken = mSource.get(s-1);
            for (int t = 1; t <= mTarget.size(); ++t) {
                T targetToken = mTarget.get(t-1);
                int cost = match(sourceToken, targetToken) ? 0 : 1;

                Entry e = get(s - 1, t);
                int distance = e.getDistance() + 1;
                int type = EDIT_DELETE;

                e = get(s, t - 1);
                if (e.getDistance() + 1 < distance ) {
                    distance = e.getDistance() + 1;
                    type = EDIT_INSERT;
                }

                e = get(s - 1, t - 1);
                if (e.getDistance() + cost < distance) {
                    distance = e.getDistance() + cost;
                    type = cost == 0 ? EDIT_UNCHANGED : EDIT_REPLACE;
                }
                set(s, t, type, distance);
            }
        }
        return get(mSource.size(), mTarget.size()).getDistance();
    }

    /**
     * Gets the list of operations which were applied to each target token; {@link #calculate} must
     * have been called on this object before using this method.
     * @return A list of {@link EditOperation}s indicating the origin of each token in the target
     *      string. The position of the token indicates the position in the source string of the
     *      token that was unchanged/replaced, or the position in the source after which a target
     *      token was inserted.
     */
    public EditOperation[] getTargetOperations() {
        EditOperation[] ops = new EditOperation[mTarget.size()];
        int targetPos = mTarget.size();
        int sourcePos = mSource.size();
        while (targetPos > 0) {
            Entry e = get(sourcePos, targetPos);
            int editType = e.getEditType();
            switch (editType) {
                case LevenshteinDistance.EDIT_DELETE:
                    sourcePos--;
                    break;
                case LevenshteinDistance.EDIT_INSERT:
                    targetPos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
                case LevenshteinDistance.EDIT_UNCHANGED:
                case LevenshteinDistance.EDIT_REPLACE:
                    targetPos--;
                    sourcePos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
            }
        }

        return ops;
    }

    private void set(int sourceIdx, int targetIdx, int editType, int distance) {
        mTable[sourceIdx][targetIdx] = new Entry(editType, distance);
    }

    private Entry get(int sourceIdx, int targetIdx) {
        Entry e = mTable[sourceIdx][targetIdx];
        if (e == null) {
            throw new NullPointerException("No entry at " + sourceIdx + "," + targetIdx +
                    "; size: " + (mSource.size() + 1) + "," + (mTarget.size() + 1));
        }
        return e;
    }

    private static class Entry {
        private final int mEditType;
        private final int mDistance;
        public Entry(int editType, int distance) {
            mEditType = editType;
            mDistance = distance;
        }
        public int getDistance() {
            return mDistance;
        }
        public int getEditType() {
            return mEditType;
        }
    }

    public static class EditOperation {
        private final int mType;
        private final int mPosition;
        public EditOperation(int type, int position) {
            mType = type;
            mPosition = position;
        }
        public int getType() {
            return mType;
        }
        public int getPosition() {
            return mPosition;
        }
    }

}
