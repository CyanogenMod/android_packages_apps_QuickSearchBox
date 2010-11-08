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
import com.google.common.annotations.VisibleForTesting;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * System search settings fragment.
 */
@VisibleForTesting
public class SystemSearchFragment extends PreferenceFragment {

    private SystemSearchSettingsController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mController = createController();

        getPreferenceManager().setSharedPreferencesName(mController.getPreferencesName());

        addPreferencesFromResource(R.xml.system_search_preferences);

        mController.setClearShortcutsPreference((OkCancelPreference)
                findPreference(mController.getClearShortcutsPreferenceName()));

        mController.setCorporaPreference(findPreference(mController.getCorporaPreferenceName()));

    }

    protected SystemSearchSettingsController createController() {
        QsbApplication app = QsbApplication.get(getActivity());
        return new SystemSearchSettingsController(app.getSettings(), app.getShortcutRepository());
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.onResume();
    }
}
