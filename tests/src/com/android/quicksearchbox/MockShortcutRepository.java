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

import java.util.List;
import java.util.Map;

/**
 * Mock implementation of {@link ShortcutRepository}.
 *
 */
public class MockShortcutRepository implements ShortcutRepository {

    public void clearHistory() {
    }

    public void close() {
    }

    public SuggestionCursor getShortcutsForQuery(String query, List<Corpus> corporaToQuery,
            int maxShortcuts) {
        // TODO: should look at corporaToQuery
        DataSuggestionCursor cursor = new DataSuggestionCursor(query);
        SuggestionData s1 = new SuggestionData(MockSource.SOURCE_1);
        s1.setText1(query + "_1_shortcut");
        SuggestionData s2 = new SuggestionData(MockSource.SOURCE_2);
        s2.setText1(query + "_2_shortcut");
        cursor.add(s1);
        cursor.add(s2);
        return cursor;
    }

    public Map<String, Integer> getCorpusScores() {
        return null;
    }

    public boolean hasHistory() {
        return false;
    }

    public void reportClick(SuggestionCursor suggestions, int position) {
    }

}
