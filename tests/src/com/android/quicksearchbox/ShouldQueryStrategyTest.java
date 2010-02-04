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

import android.test.AndroidTestCase;

/**
 * Tests for {@link ShouldQueryStrategy}.
 */
public class ShouldQueryStrategyTest extends AndroidTestCase {

    private ShouldQueryStrategy mShouldQuery;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mShouldQuery = new ShouldQueryStrategy();
    }

    public static final Source SOURCE_1 = new MockSource("SOURCE_1") {
        @Override
        public int getQueryThreshold() {
            return 3;
        }

    };

    public static final Source SOURCE_2 = new MockSource("SOURCE_1") {
        @Override
        public boolean queryAfterZeroResults() {
            return true;
        }
    };

    public void testRespectsQueryThreshold() {
        assertFalse(mShouldQuery.shouldQuerySource(SOURCE_1, "aa"));
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_1, "aaa"));
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_2, ""));
    }

    public void testQueriesAfterNoResultsWhenQueryAfterZeroIsTrue() {
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_2, "query"));
        mShouldQuery.onZeroResults(SOURCE_2.getComponentName(), "query");
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_2, "query"));
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_2, "query123"));
    }

    public void testDoesntQueryAfterNoResultsWhenQueryAfterZeroIsFalse() {
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_1, "query"));
        mShouldQuery.onZeroResults(SOURCE_1.getComponentName(), "query");
        // Now we don't query for queries starting with "query"
        assertFalse(mShouldQuery.shouldQuerySource(SOURCE_1, "query"));
        assertFalse(mShouldQuery.shouldQuerySource(SOURCE_1, "query123"));
        // But we do query for something shorter
        assertTrue(mShouldQuery.shouldQuerySource(SOURCE_1, "quer"));
    }
}
