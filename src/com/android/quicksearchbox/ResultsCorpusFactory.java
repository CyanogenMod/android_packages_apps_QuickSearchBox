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

import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.util.Factory;

import android.content.Context;

import java.util.concurrent.Executor;


/**
 * Corpus factory that does not include web suggestions in its results.
 */
public class ResultsCorpusFactory extends SearchableCorpusFactory {

    public ResultsCorpusFactory(Context context, Config config,
            Factory<Executor> webCorpusExecutorFactory) {
        super(context, config, webCorpusExecutorFactory);
    }

    @Override
    protected Source getWebSource(Sources sources) {
        Source s = super.getWebSource(sources);
        if (s instanceof GoogleSource) {
            s = ((GoogleSource) s).getNonWebSuggestSource();
        }
        return s;
    }

}
