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




/**
 *
 */
public class SingleSourceSuggestions extends Suggestions {

    SourceResult mResult;

    public SingleSourceSuggestions(String query) {
        super(query);
    }

    @Override
    public void close() {
        super.close();
        if (mResult != null) {
            mResult.close();
            mResult = null;
        }
    }

    @Override
    public SuggestionCursor getPromoted() {
        return mResult;
    }

    @Override
    public boolean isDone() {
        return mResult != null;
    }

    public void setResult(SourceResult result) {
        if (mResult != null) throw new IllegalStateException("Already have a result");
        if (isClosed()) {
            result.close();
            return;
        }
        mResult = result;
        notifyDataSetChanged();
    }

}
