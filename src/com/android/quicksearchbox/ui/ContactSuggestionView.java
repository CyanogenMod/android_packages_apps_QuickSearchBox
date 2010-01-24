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

package com.android.quicksearchbox.ui;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SuggestionCursor;

/**
 * View for contacts appearing in the suggestions list.
 */
public class ContactSuggestionView extends DefaultSuggestionView {

    private QuickContactBadge mQuickContact;

    public ContactSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ContactSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactSuggestionView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQuickContact = (QuickContactBadge) findViewById(R.id.icon1);
    }

    @Override
    public void bindAsSuggestion(SuggestionCursor suggestion) {
        super.bindAsSuggestion(suggestion);
        mQuickContact.assignContactUri(Uri.parse(suggestion.getSuggestionIntentDataString()));
    }
}