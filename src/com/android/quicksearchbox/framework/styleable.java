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
package com.android.quicksearchbox.framework;

// This code is copied from code generated in the Android build in
// out/target/common/R/android/R.java

public class styleable {
    /**  Searchable activities and applications must provide search configuration information
    in an XML file, typically called searchable.xml.  This file is referenced in your manifest.
    For a more in-depth discussion of search configuration, please refer to
    {@link android.app.SearchManager}. 
       <p>Includes the following attributes:</p>
       <table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
       <colgroup align="left" />
       <colgroup align="left" />
       <tr><th>Attribute<th>Summary</tr>
       <tr><th><code>{@link #Searchable_autoUrlDetect android:autoUrlDetect}</code><td> If provided and <code>true</code>, URLs entered in the search dialog while searching
         within this activity would be detected and treated as URLs (show a 'go' button in the
         keyboard and invoke the browser directly when user launches the URL instead of passing
         the URL to the activity).</tr>
       <tr><th><code>{@link #Searchable_hint android:hint}</code><td> If supplied, this string will be displayed as a hint to the user.</tr>
       <tr><th><code>{@link #Searchable_icon android:icon}</code><td> If provided, this icon will be shown in place of the label above the search box.</tr>
       <tr><th><code>{@link #Searchable_imeOptions android:imeOptions}</code><td> Additional features you can enable in an IME associated with an editor
     to improve the integration with your application.</tr>
       <tr><th><code>{@link #Searchable_includeInGlobalSearch android:includeInGlobalSearch}</code><td> If provided and <code>true</code>, this searchable activity will be
         included in any global lists of search targets.</tr>
       <tr><th><code>{@link #Searchable_inputType android:inputType}</code><td> The type of data being placed in a text field, used to help an
     input method decide how to let the user enter text.</tr>
       <tr><th><code>{@link #Searchable_label android:label}</code><td> This is the user-displayed name of the searchable activity.</tr>
       <tr><th><code>{@link #Searchable_queryAfterZeroResults android:queryAfterZeroResults}</code><td> If provided and <code>true</code>, this searchable activity will be invoked for all
         queries in a particular session.</tr>
       <tr><th><code>{@link #Searchable_searchButtonText android:searchButtonText}</code><td> If supplied, this string will be displayed as the text of the "Search" button.</tr>
       <tr><th><code>{@link #Searchable_searchMode android:searchMode}</code><td> Additional features are controlled by mode bits in this field.</tr>
       <tr><th><code>{@link #Searchable_searchSettingsDescription android:searchSettingsDescription}</code><td> If provided, this string will be used to describe the searchable item in the
         searchable items settings within system search settings.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestAuthority android:searchSuggestAuthority}</code><td> If provided, this is the trigger indicating that the searchable activity
        provides suggestions as well.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestIntentAction android:searchSuggestIntentAction}</code><td> If provided, and not overridden by an action in the selected suggestion, this
        string will be placed in the action field of the {@link android.content.Intent Intent}
        when the user clicks a suggestion.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestIntentData android:searchSuggestIntentData}</code><td> If provided, and not overridden by an action in the selected suggestion, this
        string will be placed in the data field of the {@link android.content.Intent Intent}
        when the user clicks a suggestion.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestPath android:searchSuggestPath}</code><td> If provided, this will be inserted in the suggestions query Uri, after the authority
        you have provide but before the standard suggestions path.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestSelection android:searchSuggestSelection}</code><td> If provided, suggestion queries will be passed into your query function
        as the <i>selection</i> parameter.</tr>
       <tr><th><code>{@link #Searchable_searchSuggestThreshold android:searchSuggestThreshold}</code><td> If provided, this is the minimum number of characters needed to trigger
         search suggestions.</tr>
       <tr><th><code>{@link #Searchable_voiceLanguage android:voiceLanguage}</code><td> If provided, this specifies the spoken language to be expected, and that it will be
         different than the one set in the {@link java.util.Locale#getDefault()}.</tr>
       <tr><th><code>{@link #Searchable_voiceLanguageModel android:voiceLanguageModel}</code><td> If provided, this specifies the language model that should be used by the
         voice recognition system.</tr>
       <tr><th><code>{@link #Searchable_voiceMaxResults android:voiceMaxResults}</code><td> If provided, enforces the maximum number of results to return, including the "best"
         result which will always be provided as the SEARCH intent's primary query.</tr>
       <tr><th><code>{@link #Searchable_voicePromptText android:voicePromptText}</code><td> If provided, this specifies a prompt that will be displayed during voice input.</tr>
       <tr><th><code>{@link #Searchable_voiceSearchMode android:voiceSearchMode}</code><td> Voice search features are controlled by mode bits in this field.</tr>
       </table>
       @see #Searchable_autoUrlDetect
       @see #Searchable_hint
       @see #Searchable_icon
       @see #Searchable_imeOptions
       @see #Searchable_includeInGlobalSearch
       @see #Searchable_inputType
       @see #Searchable_label
       @see #Searchable_queryAfterZeroResults
       @see #Searchable_searchButtonText
       @see #Searchable_searchMode
       @see #Searchable_searchSettingsDescription
       @see #Searchable_searchSuggestAuthority
       @see #Searchable_searchSuggestIntentAction
       @see #Searchable_searchSuggestIntentData
       @see #Searchable_searchSuggestPath
       @see #Searchable_searchSuggestSelection
       @see #Searchable_searchSuggestThreshold
       @see #Searchable_voiceLanguage
       @see #Searchable_voiceLanguageModel
       @see #Searchable_voiceMaxResults
       @see #Searchable_voicePromptText
       @see #Searchable_voiceSearchMode
     */
    public static final int[] Searchable = {
        0x01010001, 0x01010002, 0x01010150, 0x010101d5,
        0x010101d6, 0x010101d7, 0x010101d8, 0x010101d9,
        0x010101da, 0x01010205, 0x01010220, 0x01010252,
        0x01010253, 0x01010254, 0x01010255, 0x01010256,
        0x01010264, 0x0101026d, 0x0101026e, 0x01010282,
        0x0101028a, 0x0101028c
    };
    /**
      <p>
      @attr description
       If provided and <code>true</code>, URLs entered in the search dialog while searching
         within this activity would be detected and treated as URLs (show a 'go' button in the
         keyboard and invoke the browser directly when user launches the URL instead of passing
         the URL to the activity). If set to <code>false</code> any URLs entered are treated as
         normal query text.
         The default value is <code>false</code>. <i>Optional attribute.</i>. 


      <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#autoUrlDetect}.
      @attr name android:autoUrlDetect
    */
    public static final int Searchable_autoUrlDetect = 21;
    /**
      <p>
      @attr description
       If supplied, this string will be displayed as a hint to the user.  <i>Optional
        attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#hint}.
      @attr name android:hint
    */
    public static final int Searchable_hint = 2;
    /**
      <p>
      @attr description
       If provided, this icon will be shown in place of the label above the search box.
         This is a reference to a drawable (icon) resource. Note that the application icon
         is also used as an icon to the left of the search box and you cannot modify this
         behavior, so including the icon attribute is unecessary and this may be
         deprecated in the future.
         <i>Optional attribute.</i> 


      <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#icon}.
      @attr name android:icon
    */
    public static final int Searchable_icon = 1;
    /**
      <p>
      @attr description
       Additional features you can enable in an IME associated with an editor
     to improve the integration with your application.  The constants
     here correspond to those defined by
     {@link android.view.inputmethod.EditorInfo#imeOptions}. 


      <p>Must be one or more (separated by '|') of the following constant values.</p>
<table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
<colgroup align="left" />
<colgroup align="left" />
<colgroup align="left" />
<tr><th>Constant<th>Value<th>Description</tr>
<tr><th><code>normal</code><td>0x00000000<td> There are no special semantics associated with this editor. </tr>
<tr><th><code>actionUnspecified</code><td>0x00000000<td> There is no specific action associated with this editor, let the
         editor come up with its own if it can.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_NULL}. </tr>
<tr><th><code>actionNone</code><td>0x00000001<td> This editor has no action associated with it.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_NONE}. </tr>
<tr><th><code>actionGo</code><td>0x00000002<td> The action key performs a "go"
         operation to take the user to the target of the text they typed.
         Typically used, for example, when entering a URL.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_GO}. </tr>
<tr><th><code>actionSearch</code><td>0x00000003<td> The action key performs a "search"
         operation, taking the user to the results of searching for the text
         the have typed (in whatever context is appropriate).
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_SEARCH}. </tr>
<tr><th><code>actionSend</code><td>0x00000004<td> The action key performs a "send"
         operation, delivering the text to its target.  This is typically used
         when composing a message.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_SEND}. </tr>
<tr><th><code>actionNext</code><td>0x00000005<td> The action key performs a "next"
         operation, taking the user to the next field that will accept text.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_NEXT}. </tr>
<tr><th><code>actionDone</code><td>0x00000006<td> The action key performs a "done"
         operation, closing the soft input method.
         Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_ACTION_DONE}. </tr>
<tr><th><code>flagNoExtractUi</code><td>0x10000000<td> Used to specify that the IME does not need
         to show its extracted text UI.  For input methods that may be fullscreen,
         often when in landscape mode, this allows them to be smaller and let part
         of the application be shown behind.  Though there will likely be limited
         access to the application available from the user, it can make the
         experience of a (mostly) fullscreen IME less jarring.  Note that when
         this flag is specified the IME may <em>not</em> be set up to be able
         to display text, so it should only be used in situations where this is
         not needed.
         <p>Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_FLAG_NO_EXTRACT_UI}. </tr>
<tr><th><code>flagNoAccessoryAction</code><td>0x20000000<td> Used in conjunction with a custom action, this indicates that the
         action should not be available as an accessory button when the
         input method is full-screen.
         Note that by setting this flag, there can be cases where the action
         is simply never available to the user.  Setting this generally means
         that you think showing text being edited is more important than the
         action you have supplied.
         <p>Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_FLAG_NO_ACCESSORY_ACTION}. </tr>
<tr><th><code>flagNoEnterAction</code><td>0x40000000<td> Used in conjunction with a custom action,
         this indicates that the action should not be available in-line as
         a replacement for the "enter" key.  Typically this is
         because the action has such a significant impact or is not recoverable
         enough that accidentally hitting it should be avoided, such as sending
         a message.    Note that {@link android.widget.TextView} will
         automatically set this flag for you on multi-line text views.
         <p>Corresponds to
         {@link android.view.inputmethod.EditorInfo#IME_FLAG_NO_ENTER_ACTION}. </tr>
</table>
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#imeOptions}.
      @attr name android:imeOptions
    */
    public static final int Searchable_imeOptions = 16;
    /**
      <p>
      @attr description
       If provided and <code>true</code>, this searchable activity will be
         included in any global lists of search targets.
         The default value is <code>false</code>. <i>Optional attribute.</i>. 


      <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#includeInGlobalSearch}.
      @attr name android:includeInGlobalSearch
    */
    public static final int Searchable_includeInGlobalSearch = 18;
    /**
      <p>
      @attr description
       The type of data being placed in a text field, used to help an
     input method decide how to let the user enter text.  The constants
     here correspond to those defined by
     {@link android.text.InputType}.  Generally you can select
     a single value, though some can be combined together as
     indicated.  Setting this attribute to anything besides
     <var>none</var> also implies that the text is editable. 


      <p>Must be one or more (separated by '|') of the following constant values.</p>
<table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
<colgroup align="left" />
<colgroup align="left" />
<colgroup align="left" />
<tr><th>Constant<th>Value<th>Description</tr>
<tr><th><code>none</code><td>0x00000000<td> There is no content type.  The text is not editable. </tr>
<tr><th><code>text</code><td>0x00000001<td> Just plain old text.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_NORMAL}. </tr>
<tr><th><code>textCapCharacters</code><td>0x00001001<td> Can be combined with <var>text</var> and its variations to
         request capitalization of all characters.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_CAP_CHARACTERS}. </tr>
<tr><th><code>textCapWords</code><td>0x00002001<td> Can be combined with <var>text</var> and its variations to
         request capitalization of the first character of every word.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_CAP_WORDS}. </tr>
<tr><th><code>textCapSentences</code><td>0x00004001<td> Can be combined with <var>text</var> and its variations to
         request capitalization of the first character of every sentence.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_CAP_SENTENCES}. </tr>
<tr><th><code>textAutoCorrect</code><td>0x00008001<td> Can be combined with <var>text</var> and its variations to
         request auto-correction of text being input.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_AUTO_CORRECT}. </tr>
<tr><th><code>textAutoComplete</code><td>0x00010001<td> Can be combined with <var>text</var> and its variations to
         specify that this field will be doing its own auto-completion and
         talking with the input method appropriately.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_AUTO_COMPLETE}. </tr>
<tr><th><code>textMultiLine</code><td>0x00020001<td> Can be combined with <var>text</var> and its variations to
         allow multiple lines of text in the field.  If this flag is not set,
         the text field will be constrained to a single line.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_MULTI_LINE}. </tr>
<tr><th><code>textImeMultiLine</code><td>0x00040001<td> Can be combined with <var>text</var> and its variations to
         indicate that though the regular text view should not be multiple
         lines, the IME should provide multiple lines if it can.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_IME_MULTI_LINE}. </tr>
<tr><th><code>textNoSuggestions</code><td>0x00080001<td> Can be combined with <var>text</var> and its variations to
         indicate that the IME should not show any
         dictionary-based word suggestions.  Corresponds to
         {@link android.text.InputType#TYPE_TEXT_FLAG_NO_SUGGESTIONS}. </tr>
<tr><th><code>textUri</code><td>0x00000011<td> Text that will be used as a URI.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_URI}. </tr>
<tr><th><code>textEmailAddress</code><td>0x00000021<td> Text that will be used as an e-mail address.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_EMAIL_ADDRESS}. </tr>
<tr><th><code>textEmailSubject</code><td>0x00000031<td> Text that is being supplied as the subject of an e-mail.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_EMAIL_SUBJECT}. </tr>
<tr><th><code>textShortMessage</code><td>0x00000041<td> Text that is the content of a short message.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_SHORT_MESSAGE}. </tr>
<tr><th><code>textLongMessage</code><td>0x00000051<td> Text that is the content of a long message.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_LONG_MESSAGE}. </tr>
<tr><th><code>textPersonName</code><td>0x00000061<td> Text that is the name of a person.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_PERSON_NAME}. </tr>
<tr><th><code>textPostalAddress</code><td>0x00000071<td> Text that is being supplied as a postal mailing address.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_POSTAL_ADDRESS}. </tr>
<tr><th><code>textPassword</code><td>0x00000081<td> Text that is a password.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_PASSWORD}. </tr>
<tr><th><code>textVisiblePassword</code><td>0x00000091<td> Text that is a password that should be visible.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_VISIBLE_PASSWORD}. </tr>
<tr><th><code>textWebEditText</code><td>0x000000a1<td> Text that is being supplied as text in a web form.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_WEB_EDIT_TEXT}. </tr>
<tr><th><code>textFilter</code><td>0x000000b1<td> Text that is filtering some other data.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_FILTER}. </tr>
<tr><th><code>textPhonetic</code><td>0x000000c1<td> Text that is for phonetic pronunciation, such as a phonetic name
         field in a contact entry.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_TEXT} |
         {@link android.text.InputType#TYPE_TEXT_VARIATION_PHONETIC}. </tr>
<tr><th><code>number</code><td>0x00000002<td> A numeric only field.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_NUMBER}. </tr>
<tr><th><code>numberSigned</code><td>0x00001002<td> Can be combined with <var>number</var> and its other options to
         allow a signed number.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_NUMBER} |
         {@link android.text.InputType#TYPE_NUMBER_FLAG_SIGNED}. </tr>
<tr><th><code>numberDecimal</code><td>0x00002002<td> Can be combined with <var>number</var> and its other options to
         allow a decimal (fractional) number.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_NUMBER} |
         {@link android.text.InputType#TYPE_NUMBER_FLAG_DECIMAL}. </tr>
<tr><th><code>phone</code><td>0x00000003<td> For entering a phone number.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_PHONE}. </tr>
<tr><th><code>datetime</code><td>0x00000004<td> For entering a date and time.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_DATETIME} |
         {@link android.text.InputType#TYPE_DATETIME_VARIATION_NORMAL}. </tr>
<tr><th><code>date</code><td>0x00000014<td> For entering a date.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_DATETIME} |
         {@link android.text.InputType#TYPE_DATETIME_VARIATION_DATE}. </tr>
<tr><th><code>time</code><td>0x00000024<td> For entering a time.  Corresponds to
         {@link android.text.InputType#TYPE_CLASS_DATETIME} |
         {@link android.text.InputType#TYPE_DATETIME_VARIATION_TIME}. </tr>
</table>
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#inputType}.
      @attr name android:inputType
    */
    public static final int Searchable_inputType = 10;
    /**
      <p>
      @attr description
       This is the user-displayed name of the searchable activity.  <i>Required
        attribute.</i> 


      <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
<p>May be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#label}.
      @attr name android:label
    */
    public static final int Searchable_label = 0;
    /**
      <p>
      @attr description
       If provided and <code>true</code>, this searchable activity will be invoked for all
         queries in a particular session. If set to <code>false</code> and the activity
         returned zero results for a query, it will not be invoked again in that session for
         supersets of that zero-results query. For example, if the activity returned zero
         results for "bo", it would not be queried again for "bob".
         The default value is <code>false</code>. <i>Optional attribute.</i>. 


      <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#queryAfterZeroResults}.
      @attr name android:queryAfterZeroResults
    */
    public static final int Searchable_queryAfterZeroResults = 19;
    /**
      <p>
      @attr description
       If supplied, this string will be displayed as the text of the "Search" button.
      <i>Optional attribute.</i>
      {@deprecated This will create a non-standard UI appearance, because the search bar UI is
                   changing to use only icons for its buttons.}


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchButtonText}.
      @attr name android:searchButtonText
    */
    @Deprecated
    public static final int Searchable_searchButtonText = 9;
    /**
      <p>
      @attr description
       Additional features are controlled by mode bits in this field.  Omitting
        this field, or setting to zero, provides default behavior.  <i>Optional attribute.</i>
    


      <p>Must be one or more (separated by '|') of the following constant values.</p>
<table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
<colgroup align="left" />
<colgroup align="left" />
<colgroup align="left" />
<tr><th>Constant<th>Value<th>Description</tr>
<tr><th><code>showSearchLabelAsBadge</code><td>0x04<td> If set, this flag enables the display of the search target (label) within the
           search bar.  If neither bad mode is selected, no badge will be shown. </tr>
<tr><th><code>showSearchIconAsBadge</code><td>0x08<td> If set, this flag enables the display of the search target (icon) within the
           search bar.  (Note, overrides showSearchLabel)  If neither bad mode is selected,
           no badge will be shown.</tr>
<tr><th><code>queryRewriteFromData</code><td>0x10<td> If set, this flag causes the suggestion column SUGGEST_COLUMN_INTENT_DATA to
           be considered as the text for suggestion query rewriting.  This should only
           be used when the values in SUGGEST_COLUMN_INTENT_DATA are suitable for user
           inspection and editing - typically, HTTP/HTTPS Uri's. </tr>
<tr><th><code>queryRewriteFromText</code><td>0x20<td> If set, this flag causes the suggestion column SUGGEST_COLUMN_TEXT_1 to
           be considered as the text for suggestion query rewriting.  This should be used
           for suggestions in which no query text is provided and the SUGGEST_COLUMN_INTENT_DATA
           values are not suitable for user inspection and editing. </tr>
</table>
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchMode}.
      @attr name android:searchMode
    */
    public static final int Searchable_searchMode = 3;
    /**
      <p>
      @attr description
       If provided, this string will be used to describe the searchable item in the
         searchable items settings within system search settings. <i>Optional
         attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSettingsDescription}.
      @attr name android:searchSettingsDescription
    */
    public static final int Searchable_searchSettingsDescription = 20;
    /**
      <p>
      @attr description
       If provided, this is the trigger indicating that the searchable activity
        provides suggestions as well.  The value must be a fully-qualified content provider
        authority (e.g. "com.example.android.apis.SuggestionProvider") and should match the
        "android:authorities" tag in your content provider's manifest entry.  <i>Optional
        attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestAuthority}.
      @attr name android:searchSuggestAuthority
    */
    public static final int Searchable_searchSuggestAuthority = 4;
    /**
      <p>
      @attr description
       If provided, and not overridden by an action in the selected suggestion, this
        string will be placed in the action field of the {@link android.content.Intent Intent}
        when the user clicks a suggestion.  <i>Optional attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestIntentAction}.
      @attr name android:searchSuggestIntentAction
    */
    public static final int Searchable_searchSuggestIntentAction = 7;
    /**
      <p>
      @attr description
       If provided, and not overridden by an action in the selected suggestion, this
        string will be placed in the data field of the {@link android.content.Intent Intent}
        when the user clicks a suggestion.  <i>Optional attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestIntentData}.
      @attr name android:searchSuggestIntentData
    */
    public static final int Searchable_searchSuggestIntentData = 8;
    /**
      <p>
      @attr description
       If provided, this will be inserted in the suggestions query Uri, after the authority
        you have provide but before the standard suggestions path. <i>Optional attribute.</i>
        


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestPath}.
      @attr name android:searchSuggestPath
    */
    public static final int Searchable_searchSuggestPath = 5;
    /**
      <p>
      @attr description
       If provided, suggestion queries will be passed into your query function
        as the <i>selection</i> parameter.  Typically this will be a WHERE clause for your
        database, and will contain a single question mark, which represents the actual query
        string that has been typed by the user.  If not provided, then the user query text
        will be appended to the query Uri (after an additional "/".)  <i>Optional
        attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestSelection}.
      @attr name android:searchSuggestSelection
    */
    public static final int Searchable_searchSuggestSelection = 6;
    /**
      <p>
      @attr description
       If provided, this is the minimum number of characters needed to trigger
         search suggestions. The default value is 0. <i>Optional attribute.</i> 


      <p>Must be an integer value, such as "<code>100</code>".
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#searchSuggestThreshold}.
      @attr name android:searchSuggestThreshold
    */
    public static final int Searchable_searchSuggestThreshold = 17;
    /**
      <p>
      @attr description
       If provided, this specifies the spoken language to be expected, and that it will be
         different than the one set in the {@link java.util.Locale#getDefault()}. 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#voiceLanguage}.
      @attr name android:voiceLanguage
    */
    public static final int Searchable_voiceLanguage = 14;
    /**
      <p>
      @attr description
       If provided, this specifies the language model that should be used by the
         voice recognition system.  See
         {@link android.speech.RecognizerIntent#EXTRA_LANGUAGE_MODEL } for more information.
         If not provided, the default value
         {@link android.speech.RecognizerIntent#LANGUAGE_MODEL_FREE_FORM } will be used. 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#voiceLanguageModel}.
      @attr name android:voiceLanguageModel
    */
    public static final int Searchable_voiceLanguageModel = 12;
    /**
      <p>
      @attr description
       If provided, enforces the maximum number of results to return, including the "best"
         result which will always be provided as the SEARCH intent's primary query.  Must be one
         or greater.  If not provided, the recognizer will choose how many results to return.
         


      <p>Must be an integer value, such as "<code>100</code>".
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#voiceMaxResults}.
      @attr name android:voiceMaxResults
    */
    public static final int Searchable_voiceMaxResults = 15;
    /**
      <p>
      @attr description
       If provided, this specifies a prompt that will be displayed during voice input. 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#voicePromptText}.
      @attr name android:voicePromptText
    */
    public static final int Searchable_voicePromptText = 13;
    /**
      <p>
      @attr description
       Voice search features are controlled by mode bits in this field.  Omitting
        this field, or setting to zero, provides default behavior.
        If showVoiceSearchButton is set, then launchWebSearch or launchRecognizer must
        also be set.  <i>Optional attribute.</i>
    


      <p>Must be one or more (separated by '|') of the following constant values.</p>
<table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
<colgroup align="left" />
<colgroup align="left" />
<colgroup align="left" />
<tr><th>Constant<th>Value<th>Description</tr>
<tr><th><code>showVoiceSearchButton</code><td>0x01<td> If set, display a voice search button.  This only takes effect if voice search is
           available on the device. </tr>
<tr><th><code>launchWebSearch</code><td>0x02<td> If set, the voice search button will take the user directly to a built-in
           voice web search activity.  Most applications will not use this flag, as it
           will take the user away from the activity in which search was invoked. </tr>
<tr><th><code>launchRecognizer</code><td>0x04<td> If set, the voice search button will take the user directly to a built-in
           voice recording activity.  This activity will prompt the user to speak,
           transcribe the spoken text, and forward the resulting query
           text to the searchable activity, just as if the user had typed it into
           the search UI and clicked the search button. </tr>
</table>
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#voiceSearchMode}.
      @attr name android:voiceSearchMode
    */
    public static final int Searchable_voiceSearchMode = 11;
    /**  In order to process special action keys during search, you must define them using
        one or more "ActionKey" elements in your Searchable metadata.  For a more in-depth
        discussion of action code handling, please refer to {@link android.app.SearchManager}.

       <p>Includes the following attributes:</p>
       <table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
       <colgroup align="left" />
       <colgroup align="left" />
       <tr><th>Attribute<th>Summary</tr>
       <tr><th><code>{@link #SearchableActionKey_keycode android:keycode}</code><td> This attribute denotes the action key you wish to respond to.</tr>
       <tr><th><code>{@link #SearchableActionKey_queryActionMsg android:queryActionMsg}</code><td> If you wish to handle an action key during normal search query entry, you
        must define an action string here.</tr>
       <tr><th><code>{@link #SearchableActionKey_suggestActionMsg android:suggestActionMsg}</code><td> If you wish to handle an action key while a suggestion is being displayed <i>and
        selected</i>, there are two ways to handle this.</tr>
       <tr><th><code>{@link #SearchableActionKey_suggestActionMsgColumn android:suggestActionMsgColumn}</code><td> If you wish to handle an action key while a suggestion is being displayed <i>and
        selected</i>, but you do not wish to enable this action key for every suggestion,
        then you can use this attribute to control it on a suggestion-by-suggestion basis.</tr>
       </table>
       @see #SearchableActionKey_keycode
       @see #SearchableActionKey_queryActionMsg
       @see #SearchableActionKey_suggestActionMsg
       @see #SearchableActionKey_suggestActionMsgColumn
     */
    public static final int[] SearchableActionKey = {
        0x010100c5, 0x010101db, 0x010101dc, 0x010101dd
    };
    /**
      <p>
      @attr description
       This attribute denotes the action key you wish to respond to.  Note that not
        all action keys are actually supported using this mechanism, as many of them are
        used for typing, navigation, or system functions.  This will be added to the
        {@link android.content.Intent#ACTION_SEARCH ACTION_SEARCH} intent that is passed to your
        searchable activity.  To examine the key code, use
        {@link android.content.Intent#getIntExtra getIntExtra(SearchManager.ACTION_KEY)}.
        <p>Note, in addition to the keycode, you must also provide one or more of the action
        specifier attributes.  <i>Required attribute.</i> 


      <p>Must be one of the following constant values.</p>
<table border="2" width="85%" align="center" frame="hsides" rules="all" cellpadding="5">
<colgroup align="left" />
<colgroup align="left" />
<colgroup align="left" />
<tr><th>Constant<th>Value<th>Description</tr>
<tr><th><code>KEYCODE_UNKNOWN</code><td>0<td></tr>
<tr><th><code>KEYCODE_SOFT_LEFT</code><td>1<td></tr>
<tr><th><code>KEYCODE_SOFT_RIGHT</code><td>2<td></tr>
<tr><th><code>KEYCODE_HOME</code><td>3<td></tr>
<tr><th><code>KEYCODE_BACK</code><td>4<td></tr>
<tr><th><code>KEYCODE_CALL</code><td>5<td></tr>
<tr><th><code>KEYCODE_ENDCALL</code><td>6<td></tr>
<tr><th><code>KEYCODE_0</code><td>7<td></tr>
<tr><th><code>KEYCODE_1</code><td>8<td></tr>
<tr><th><code>KEYCODE_2</code><td>9<td></tr>
<tr><th><code>KEYCODE_3</code><td>10<td></tr>
<tr><th><code>KEYCODE_4</code><td>11<td></tr>
<tr><th><code>KEYCODE_5</code><td>12<td></tr>
<tr><th><code>KEYCODE_6</code><td>13<td></tr>
<tr><th><code>KEYCODE_7</code><td>14<td></tr>
<tr><th><code>KEYCODE_8</code><td>15<td></tr>
<tr><th><code>KEYCODE_9</code><td>16<td></tr>
<tr><th><code>KEYCODE_STAR</code><td>17<td></tr>
<tr><th><code>KEYCODE_POUND</code><td>18<td></tr>
<tr><th><code>KEYCODE_DPAD_UP</code><td>19<td></tr>
<tr><th><code>KEYCODE_DPAD_DOWN</code><td>20<td></tr>
<tr><th><code>KEYCODE_DPAD_LEFT</code><td>21<td></tr>
<tr><th><code>KEYCODE_DPAD_RIGHT</code><td>22<td></tr>
<tr><th><code>KEYCODE_DPAD_CENTER</code><td>23<td></tr>
<tr><th><code>KEYCODE_VOLUME_UP</code><td>24<td></tr>
<tr><th><code>KEYCODE_VOLUME_DOWN</code><td>25<td></tr>
<tr><th><code>KEYCODE_POWER</code><td>26<td></tr>
<tr><th><code>KEYCODE_CAMERA</code><td>27<td></tr>
<tr><th><code>KEYCODE_CLEAR</code><td>28<td></tr>
<tr><th><code>KEYCODE_A</code><td>29<td></tr>
<tr><th><code>KEYCODE_B</code><td>30<td></tr>
<tr><th><code>KEYCODE_C</code><td>31<td></tr>
<tr><th><code>KEYCODE_D</code><td>32<td></tr>
<tr><th><code>KEYCODE_E</code><td>33<td></tr>
<tr><th><code>KEYCODE_F</code><td>34<td></tr>
<tr><th><code>KEYCODE_G</code><td>35<td></tr>
<tr><th><code>KEYCODE_H</code><td>36<td></tr>
<tr><th><code>KEYCODE_I</code><td>37<td></tr>
<tr><th><code>KEYCODE_J</code><td>38<td></tr>
<tr><th><code>KEYCODE_K</code><td>39<td></tr>
<tr><th><code>KEYCODE_L</code><td>40<td></tr>
<tr><th><code>KEYCODE_M</code><td>41<td></tr>
<tr><th><code>KEYCODE_N</code><td>42<td></tr>
<tr><th><code>KEYCODE_O</code><td>43<td></tr>
<tr><th><code>KEYCODE_P</code><td>44<td></tr>
<tr><th><code>KEYCODE_Q</code><td>45<td></tr>
<tr><th><code>KEYCODE_R</code><td>46<td></tr>
<tr><th><code>KEYCODE_S</code><td>47<td></tr>
<tr><th><code>KEYCODE_T</code><td>48<td></tr>
<tr><th><code>KEYCODE_U</code><td>49<td></tr>
<tr><th><code>KEYCODE_V</code><td>50<td></tr>
<tr><th><code>KEYCODE_W</code><td>51<td></tr>
<tr><th><code>KEYCODE_X</code><td>52<td></tr>
<tr><th><code>KEYCODE_Y</code><td>53<td></tr>
<tr><th><code>KEYCODE_Z</code><td>54<td></tr>
<tr><th><code>KEYCODE_COMMA</code><td>55<td></tr>
<tr><th><code>KEYCODE_PERIOD</code><td>56<td></tr>
<tr><th><code>KEYCODE_ALT_LEFT</code><td>57<td></tr>
<tr><th><code>KEYCODE_ALT_RIGHT</code><td>58<td></tr>
<tr><th><code>KEYCODE_SHIFT_LEFT</code><td>59<td></tr>
<tr><th><code>KEYCODE_SHIFT_RIGHT</code><td>60<td></tr>
<tr><th><code>KEYCODE_TAB</code><td>61<td></tr>
<tr><th><code>KEYCODE_SPACE</code><td>62<td></tr>
<tr><th><code>KEYCODE_SYM</code><td>63<td></tr>
<tr><th><code>KEYCODE_EXPLORER</code><td>64<td></tr>
<tr><th><code>KEYCODE_ENVELOPE</code><td>65<td></tr>
<tr><th><code>KEYCODE_ENTER</code><td>66<td></tr>
<tr><th><code>KEYCODE_DEL</code><td>67<td></tr>
<tr><th><code>KEYCODE_GRAVE</code><td>68<td></tr>
<tr><th><code>KEYCODE_MINUS</code><td>69<td></tr>
<tr><th><code>KEYCODE_EQUALS</code><td>70<td></tr>
<tr><th><code>KEYCODE_LEFT_BRACKET</code><td>71<td></tr>
<tr><th><code>KEYCODE_RIGHT_BRACKET</code><td>72<td></tr>
<tr><th><code>KEYCODE_BACKSLASH</code><td>73<td></tr>
<tr><th><code>KEYCODE_SEMICOLON</code><td>74<td></tr>
<tr><th><code>KEYCODE_APOSTROPHE</code><td>75<td></tr>
<tr><th><code>KEYCODE_SLASH</code><td>76<td></tr>
<tr><th><code>KEYCODE_AT</code><td>77<td></tr>
<tr><th><code>KEYCODE_NUM</code><td>78<td></tr>
<tr><th><code>KEYCODE_HEADSETHOOK</code><td>79<td></tr>
<tr><th><code>KEYCODE_FOCUS</code><td>80<td></tr>
<tr><th><code>KEYCODE_PLUS</code><td>81<td></tr>
<tr><th><code>KEYCODE_MENU</code><td>82<td></tr>
<tr><th><code>KEYCODE_NOTIFICATION</code><td>83<td></tr>
<tr><th><code>KEYCODE_SEARCH</code><td>84<td></tr>
<tr><th><code>KEYCODE_MEDIA_PLAY_PAUSE</code><td>85<td></tr>
<tr><th><code>KEYCODE_MEDIA_STOP</code><td>86<td></tr>
<tr><th><code>KEYCODE_MEDIA_NEXT</code><td>87<td></tr>
<tr><th><code>KEYCODE_MEDIA_PREVIOUS</code><td>88<td></tr>
<tr><th><code>KEYCODE_MEDIA_REWIND</code><td>89<td></tr>
<tr><th><code>KEYCODE_MEDIA_FAST_FORWARD</code><td>90<td></tr>
<tr><th><code>KEYCODE_MUTE</code><td>91<td></tr>
</table>
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#keycode}.
      @attr name android:keycode
    */
    public static final int SearchableActionKey_keycode = 0;
    /**
      <p>
      @attr description
       If you wish to handle an action key during normal search query entry, you
        must define an action string here.  This will be added to the
        {@link android.content.Intent#ACTION_SEARCH ACTION_SEARCH} intent that is passed to your
        searchable activity.  To examine the string, use
        {@link android.content.Intent#getStringExtra getStringExtra(SearchManager.ACTION_MSG)}.
        <i>Optional attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#queryActionMsg}.
      @attr name android:queryActionMsg
    */
    public static final int SearchableActionKey_queryActionMsg = 1;
    /**
      <p>
      @attr description
       If you wish to handle an action key while a suggestion is being displayed <i>and
        selected</i>, there are two ways to handle this.  If <i>all</i> of your suggestions
        can handle the action key, you can simply define the action message using this
        attribute.  This will be added to the
        {@link android.content.Intent#ACTION_SEARCH ACTION_SEARCH} intent that is passed to your
        searchable activity.  To examine the string, use
        {@link android.content.Intent#getStringExtra getStringExtra(SearchManager.ACTION_MSG)}.
        <i>Optional attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#suggestActionMsg}.
      @attr name android:suggestActionMsg
    */
    public static final int SearchableActionKey_suggestActionMsg = 2;
    /**
      <p>
      @attr description
       If you wish to handle an action key while a suggestion is being displayed <i>and
        selected</i>, but you do not wish to enable this action key for every suggestion,
        then you can use this attribute to control it on a suggestion-by-suggestion basis.
        First, you must define a column (and name it here) where your suggestions will include
        the action string.  Then, in your content provider, you must provide this column, and
        when desired, provide data in this column.
        The search manager will look at your suggestion cursor, using the string
        provided here in order to select a column, and will use that to select a string from
        the cursor.  That string will be added to the
        {@link android.content.Intent#ACTION_SEARCH ACTION_SEARCH} intent that is passed to
        your searchable activity.  To examine the string, use
        {@link android.content.Intent#getStringExtra
        getStringExtra(SearchManager.ACTION_MSG)}.  <i>If the data does not exist for the
        selection suggestion, the action key will be ignored.</i><i>Optional attribute.</i> 


      <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
<p>This may also be a reference to a resource (in the form
"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
theme attribute (in the form
"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
containing a value of this type.
      <p>This corresponds to the global attribute          resource symbol {@link android.R.attr#suggestActionMsgColumn}.
      @attr name android:suggestActionMsgColumn
    */
    public static final int SearchableActionKey_suggestActionMsgColumn = 3;
}
