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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;

/**
 * Voice Search integration.
 */
public class VoiceSearch {

    private final Context mContext;

    public VoiceSearch(Context context) {
        mContext = context;
    }

    public boolean shouldShowVoiceSearch(Corpus corpus) {
        if (corpus != null && !corpus.voiceSearchEnabled()) {
            return false;
        }
        return isVoiceSearchAvailable();
    }

    private boolean isVoiceSearchAvailable() {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        ResolveInfo ri = mContext.getPackageManager().
                resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    public Intent createVoiceWebSearchIntent(Bundle appData) {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        if (appData != null) {
            intent.putExtra(SearchManager.APP_DATA, appData);
        }
        return intent;
    }

}
