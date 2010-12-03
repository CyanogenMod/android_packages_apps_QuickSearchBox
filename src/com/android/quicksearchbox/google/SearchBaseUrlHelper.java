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

package com.android.quicksearchbox.google;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchSettings;
import com.android.quicksearchbox.util.HttpHelper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.Locale;

/**
 * Helper to build the base URL for all search requests.
 */
public class SearchBaseUrlHelper {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchBaseUrlHelper";

    private static final String DOMAIN_CHECK_URL =
            "https://www.google.com/searchdomaincheck?format=domain";

    private static final long SEARCH_BASE_URL_EXPIRY_MS = 24 * 3600 * 1000L;

    private final HttpHelper mHttpHelper;
    private final Context mContext;
    private final SearchSettings mSearchSettings;

    /**
     * Note that this constructor will spawn a thread to issue a HTTP
     * request if shouldUseGoogleCom is false.
     */
    public SearchBaseUrlHelper(Context context, HttpHelper helper,
            SearchSettings searchSettings) {
        mHttpHelper = helper;
        mContext = context;
        mSearchSettings = searchSettings;

        maybeUpdateBaseUrlSetting(false);
    }

    /**
     * Update the base search url, either:
     * (a) it has never been set (first run)
     * (b) it has expired
     * (c) if the caller forces an update by setting the "force" parameter.
     *
     * @param force if true, then the URL is reset whether or not it has
     *     expired.
     */
    public void maybeUpdateBaseUrlSetting(boolean force) {
        long lastUpdateTime = mSearchSettings.getSearchBaseUrlApplyTime();
        long currentTime = System.currentTimeMillis();

        if (force || lastUpdateTime == -1 ||
                currentTime - lastUpdateTime >= SEARCH_BASE_URL_EXPIRY_MS) {
            if (mSearchSettings.shouldUseGoogleCom()) {
                setSearchBaseUrl(getDefaultBaseUrl());
            } else {
                checkSearchDomain();
            }
        }
    }

    /**
     * @return the base url for searches.
     */
    public String getSearchBaseUrl() {
        return mSearchSettings.getSearchBaseUrl();
    }

    /**
     * Issue a request to google.com/searchdomaincheck to retrieve the base
     * URL for search requests.
     */
    private void checkSearchDomain() {
        final HttpHelper.GetRequest request = new HttpHelper.GetRequest(DOMAIN_CHECK_URL);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void ... params) {
                if (DBG) Log.d(TAG, "Starting request to /searchdomaincheck");
                String domain;
                try {
                    domain = mHttpHelper.get(request);
                } catch (Exception e) {
                    if (DBG) Log.d(TAG, "Request to /searchdomaincheck failed : " + e);
                    // Swallow any exceptions thrown by the HTTP helper, in
                    // this rare case, we just use the default URL.
                    setSearchBaseUrl(getDefaultBaseUrl());

                    return null;
                }

                String searchDomain = mContext.getResources().getString(
                        R.string.google_search_base_pattern, domain,
                        GoogleSearch.getLanguage(Locale.getDefault()));
                setSearchBaseUrl(searchDomain);
                return null;
            }
        }.execute();
    }

    private String getDefaultBaseUrl() {
        return (mContext.getResources().getString(
                R.string.google_search_base,
                GoogleSearch.getLanguage(Locale.getDefault())));
    }

    private void setSearchBaseUrl(String searchDomain) {
        if (DBG) Log.d(TAG, "Setting search domain to : " + searchDomain);

        mSearchSettings.setSearchBaseUrl(searchDomain);
    }
}