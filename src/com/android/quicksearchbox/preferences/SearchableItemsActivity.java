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

package com.android.quicksearchbox.preferences;

import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchSettingsImpl;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

/**
 * Activity for selecting searchable items.
 */
public class SearchableItemsActivity extends PreferenceActivity {

    private SearchableItemsController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        QsbApplication app = QsbApplication.get(this);
        mController = new SearchableItemsController(app.getSettings(), app.getCorpora(), this);

        getPreferenceManager().setSharedPreferencesName(SearchSettingsImpl.PREFERENCES_NAME);

        addPreferencesFromResource(R.xml.preferences_searchable_items);

        mController.setCorporaPreferences((PreferenceGroup) getPreferenceScreen().findPreference(
                mController.getCorporaPreferenceKey()));

    }

}
