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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import android.view.KeyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A shortcut repository implementation that uses a log of every click.
 *
 * To inspect DB:
 * # sqlite3 /data/data/com.android.quicksearchbox/databases/qsb-log.db
 *
 * TODO: Refactor this class.
 */
public class ShortcutRepositoryImplLog implements ShortcutRepository {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.ShortcutRepositoryImplLog";

    private static final String DB_NAME = "qsb-log.db";
    private static final int DB_VERSION = 23;

    private static final String HAS_HISTORY_QUERY =
        "SELECT " + Shortcuts.intent_key.fullName + " FROM " + Shortcuts.TABLE_NAME;
    private final String mEmptyQueryShortcutQuery ;
    private final String mShortcutQuery;

    private static final String SHORTCUT_BY_ID_WHERE =
            Shortcuts.shortcut_id.name() + "=? AND " + Shortcuts.source.name() + "=?";

    private static final String SOURCE_RANKING_SQL = buildSourceRankingSql();

    private final Context mContext;
    private final Config mConfig;
    private final SourceLookup mSources;
    private final DbOpenHelper mOpenHelper;

    /**
     * Create an instance to the repo.
     */
    public static ShortcutRepository create(Context context, Config config,
            SourceLookup sources) {
        return new ShortcutRepositoryImplLog(context, config, sources, DB_NAME);
    }

    /**
     * @param context Used to create / open db
     * @param name The name of the database to create.
     */
    ShortcutRepositoryImplLog(Context context, Config config, SourceLookup sources,
            String name) {
        mContext = context;
        mConfig = config;
        mSources = sources;
        mOpenHelper = new DbOpenHelper(context, name, DB_VERSION, config);
        mEmptyQueryShortcutQuery = buildShortcutQuery(true);
        mShortcutQuery = buildShortcutQuery(false);
    }

    private String buildShortcutQuery(boolean emptyQuery) {
        // clicklog first, since that's where restrict the result set
        String tables = ClickLog.TABLE_NAME + " INNER JOIN " + Shortcuts.TABLE_NAME
                + " ON " + ClickLog.intent_key.fullName + " = " + Shortcuts.intent_key.fullName;
        String[] columns = {
            Shortcuts.intent_key.fullName,
            Shortcuts.source.fullName,
            Shortcuts.format.fullName + " AS " + SearchManager.SUGGEST_COLUMN_FORMAT,
            Shortcuts.title + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
            Shortcuts.description + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2,
            Shortcuts.icon1 + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            Shortcuts.icon2 + " AS " + SearchManager.SUGGEST_COLUMN_ICON_2,
            Shortcuts.intent_action + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
            Shortcuts.intent_data + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            Shortcuts.intent_query + " AS " + SearchManager.SUGGEST_COLUMN_QUERY,
            Shortcuts.intent_extradata + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
            Shortcuts.shortcut_id + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
        };
        // SQL expression for the time before which no clicks should be counted.
        String cutOffTime_expr = "(" + "?3" + " - " + mConfig.getMaxStatAgeMillis() + ")";
        // Avoid GLOB by using >= AND <, with some manipulation (see nextString(String)).
        // to figure out the upper bound (e.g. >= "abc" AND < "abd"
        // This allows us to use parameter binding and still take advantage of the
        // index on the query column.
        String prefixRestriction =
                ClickLog.query.fullName + " >= ?1 AND " + ClickLog.query.fullName + " < ?2";
        // Filter out clicks that are too old
        String ageRestriction = ClickLog.hit_time.fullName + " >= " + cutOffTime_expr;
        String where = (emptyQuery ? "" : prefixRestriction + " AND ") + ageRestriction;
        String groupBy = ClickLog.intent_key.fullName;
        String having = null;
        String hit_count_expr = "COUNT(" + ClickLog._id.fullName + ")";
        String last_hit_time_expr = "MAX(" + ClickLog.hit_time.fullName + ")";
        String scale_expr =
            // time (msec) from cut-off to last hit time
            "((" + last_hit_time_expr + " - " + cutOffTime_expr + ") / "
            // divided by time (sec) from cut-off to now
            // we use msec/sec to get 1000 as max score
            + (mConfig.getMaxStatAgeMillis() / 1000) + ")";
        String ordering_expr = "(" + hit_count_expr + " * " + scale_expr + ")";
        String preferLatest = "(" + last_hit_time_expr + " = (SELECT " + last_hit_time_expr +
                " FROM " + ClickLog.TABLE_NAME + " WHERE " + where + "))";
        String orderBy = preferLatest + " DESC, " + ordering_expr + " DESC";
        final String limit = Integer.toString(mConfig.getMaxShortcutsReturned());
        return SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, where, groupBy, having, orderBy, limit);
    }

    /**
     * @return sql that ranks sources by click through rate, filtering out sources without enough
     *         impressions.
     */
    private static String buildSourceRankingSql() {
        final String orderingExpr = "1000*" + SourceStats.total_clicks.name() +
                "/" + SourceStats.total_impressions.name();

        final String tables = SourceStats.TABLE_NAME;
        final String[] columns = SourceStats.COLUMNS;
        final String where = SourceStats.total_impressions + " >= $1 AND "
                + SourceStats.total_clicks + " >= $2";
        final String groupBy = null;
        final String having = null;
        final String orderBy = orderingExpr + " DESC";
        final String limit = null;
        return SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, where, groupBy, having, orderBy, limit);
    }

    protected DbOpenHelper getOpenHelper() {
        return mOpenHelper;
    }

// --------------------- Interface ShortcutRepository ---------------------

    public boolean hasHistory() {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(HAS_HISTORY_QUERY, null);
        try {
            if (DBG) Log.d(TAG, "hasHistory(): cursor=" + cursor);
            return cursor != null && cursor.getCount() > 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void clearHistory() {
        SQLiteDatabase db = getOpenHelper().getWritableDatabase();
        getOpenHelper().clearDatabase(db);
        // TODO: We should probably have a content provider for
        // the shortcuts instead. Then the SearchWidgetProvider
        // could register an observer to update the widget
        // shortcuts whenever they change.
        SearchWidgetProvider.updateSearchWidgets(mContext);
    }

    public void deleteRepository() {
        getOpenHelper().deleteDatabase();
    }

    public void close() {
        getOpenHelper().close();
    }

    public void reportClick(SuggestionPosition clicked) {
        logClick(clicked, System.currentTimeMillis());
    }

    public void reportImpressions(List<SuggestionPosition> impressions) {
        logImpressions(impressions, System.currentTimeMillis());
    }

    public SuggestionCursor getShortcutsForQuery(String query) {
        return getShortcutsForQuery(query, System.currentTimeMillis());
    }

    public ArrayList<ComponentName> getSourceRanking() {
        return getSourceRanking(mConfig.getMinImpressionsForSourceRanking(),
                mConfig.getMinClicksForSourceRanking());
    }

    public void refreshShortcut(ComponentName source, String shortcutId, SuggestionPosition refreshed) {
        if (source == null) throw new NullPointerException("source");
        if (shortcutId == null) throw new NullPointerException("shortcutId");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        String[] whereArgs = { shortcutId, source.flattenToShortString() };
        if (refreshed == null) {
            if (DBG) Log.d(TAG, "Deleting shortcut: " + shortcutId);
            db.delete(Shortcuts.TABLE_NAME, SHORTCUT_BY_ID_WHERE, whereArgs);
        } else {
            if (DBG) Log.d(TAG, "Updating shortcut: " + shortcutId);
//            ContentValues shortcut = makeShortcutRow(refreshed);
            // TODO: updateWithOnConflict() is not exposed
//            db.updateWithOnConflict(Shortcuts.TABLE_NAME, shortcut,
//                    SHORTCUT_BY_ID_WHERE, whereArgs, SQLiteDatabase.ConflictAlgorithm.REPLACE);
        }
    }

// -------------------------- end ShortcutRepository --------------------------

    SuggestionCursor getShortcutsForQuery(String query, long now) {
        if (DBG) Log.d(TAG, "getShortcutsForQuery(" + query + ")");
        String sql = query.length() == 0 ? mEmptyQueryShortcutQuery : mShortcutQuery;
        String[] params = buildShortcutQueryParams(query, now);
        if (DBG) {
            Log.d(TAG, sql);
            Log.d(TAG, Arrays.toString(params));
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(sql, params);
        return new ShortcutCursor(mSources, query, cursor);
    }

    private static class ShortcutCursor extends CursorBackedSuggestionCursor {

        private final SourceLookup mSources;

        public ShortcutCursor(SourceLookup sources, String userQuery, Cursor cursor) {
            super(userQuery, cursor);
            mSources = sources;
        }

        @Override
        // TODO: getSource() is called a lot, we should cache the Source
        // object for each position
        protected Source getSource() {
            // TODO: Using ordinal() is hacky, look up the column instead
            String srcStr = mCursor.getString(Shortcuts.source.ordinal());
            if (srcStr == null) {
                throw new NullPointerException("Missing source for shortcut.");
            }
            ComponentName srcName = ComponentName.unflattenFromString(srcStr);
            return mSources.getSourceByComponentName(srcName);
        }

    }

    /**
     * Builds a parameter list for the query returned by {@link #buildShortcutQuery(boolean)}.
     */
    private static String[] buildShortcutQueryParams(String query, long now) {
        return new String[]{ query, nextString(query), String.valueOf(now) };
    }

    /**
     * Given a string x, this method returns the least string y such that x is not a prefix of y.
     * This is useful to implement prefix filtering by comparison, since the only strings z that
     * have x as a prefix are such that z is greater than or equal to x and z is less than y.
     *
     * @param str A non-empty string. The contract above is not honored for an empty input string,
     *        since all strings have the empty string as a prefix.
     */
    private static String nextString(String str) {
        int len = str.length();
        if (len == 0) {
            return str;
        }
        // The last code point in the string. Within the Basic Multilingual Plane,
        // this is the same as str.charAt(len-1)
        int codePoint = str.codePointBefore(len);
        // This should be safe from overflow, since the largest code point
        // representable in UTF-16 is U+10FFFF.
        int nextCodePoint = codePoint + 1;
        // The index of the start of the last code point.
        // Character.charCount(codePoint) is always 1 (in the BMP) or 2
        int lastIndex = len - Character.charCount(codePoint);
        return new StringBuilder(len)
                .append(str, 0, lastIndex)  // append everything but the last code point
                .appendCodePoint(nextCodePoint)  // instead of the last code point, use successor
                .toString();
    }

    /**
     * Returns the source ranking for sources with a minimum number of impressions.
     *
     * @param minImpressions The minimum number of impressions a source must have.
     * @param minClicks The minimum number of clicks a source must have.
     * @return The list of sources, ranked by click through rate.
     */
    ArrayList<ComponentName> getSourceRanking(int minImpressions, int minClicks) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final Cursor cursor = db.rawQuery(
                SOURCE_RANKING_SQL,
                new String[] { String.valueOf(minImpressions), String.valueOf(minClicks) });
        try {
            final ArrayList<ComponentName> sources =
                    new ArrayList<ComponentName>(cursor.getCount());
            while (cursor.moveToNext()) {
                sources.add(sourceFromCursor(cursor));
            }
            return sources;
        } finally {
            cursor.close();
        }
    }

    private ComponentName sourceFromCursor(Cursor cursor) {
        return ComponentName.unflattenFromString(cursor.getString(SourceStats.component.ordinal()));
    }

    private ContentValues makeShortcutRow(SuggestionPosition suggestionPos) {
        SuggestionCursor suggestion = suggestionPos.getSuggestion();

        ComponentName source = suggestion.getSourceComponentName();
        Intent intent = suggestion.getSuggestionIntent(mContext, null, KeyEvent.KEYCODE_UNKNOWN, null);
        String intentAction = intent.getAction();
        String intentData = intent.getDataString();
        String intentQuery = intent.getStringExtra(SearchManager.QUERY);
        String intentExtraData = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);

        StringBuilder key = new StringBuilder(source.flattenToShortString());
        key.append("#");
        if (intentData != null) {
            key.append(intentData);
        }
        key.append("#");
        if (intentAction != null) {
            key.append(intentAction);
        }
        key.append("#");
        if (intentQuery != null) {
            key.append(intentQuery);
        }
        // A string of the form source#intentData#intentAction#intentQuery 
        // for use as a unique identifier of a suggestion.
        String intentKey = key.toString();

        ContentValues cv = new ContentValues();
        cv.put(Shortcuts.intent_key.name(), intentKey);
        cv.put(Shortcuts.source.name(), source.flattenToShortString());
        cv.put(Shortcuts.format.name(), suggestion.getSuggestionFormat());
        cv.put(Shortcuts.title.name(), suggestion.getSuggestionText1());
        cv.put(Shortcuts.description.name(), suggestion.getSuggestionText2());
        cv.put(Shortcuts.icon1.name(), suggestion.getSuggestionIcon1());
        cv.put(Shortcuts.icon2.name(), suggestion.getSuggestionIcon2());
        cv.put(Shortcuts.intent_action.name(), intentAction);
        cv.put(Shortcuts.intent_data.name(), intentData);
        cv.put(Shortcuts.intent_query.name(), intentQuery);
        cv.put(Shortcuts.intent_extradata.name(), intentExtraData);
        cv.put(Shortcuts.shortcut_id.name(), suggestion.getShortcutId());
        return cv;
    }

    private void logClick(SuggestionPosition clicked, long now) {
        SuggestionCursor suggestion = clicked.getSuggestion();

        if (DBG) {
            Log.d(TAG, "logClicked(" + suggestion + ")");
        }

        if (SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT.equals(suggestion.getShortcutId())) {
            if (DBG) Log.d(TAG, "clicked suggestion requested not to be shortcuted");
            return;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Add or update suggestion info
        // Since intent_key is the primary key, any existing
        // suggestion with the same source+data+action will be replaced
        if (DBG) Log.d(TAG, "Adding shortcut: " + suggestion);
        ContentValues shortcut = makeShortcutRow(clicked);
        String intentKey = shortcut.getAsString(Shortcuts.intent_key.name());
        db.replaceOrThrow(Shortcuts.TABLE_NAME, null, shortcut);

        // Log click for shortcut
        {
            final ContentValues cv = new ContentValues();
            cv.put(ClickLog.intent_key.name(), intentKey);
            cv.put(ClickLog.query.name(), suggestion.getUserQuery());
            cv.put(ClickLog.hit_time.name(), now);
            db.insertOrThrow(ClickLog.TABLE_NAME, null, cv);
        }

        // Log click for source
        {
            final ContentValues cv = new ContentValues();
            ComponentName name = suggestion.getSourceComponentName();
            cv.put(SourceLog.component.name(), name.flattenToString());
            cv.put(SourceLog.time.name(), now);
            cv.put(SourceLog.click_count.name(), 1);
            cv.put(SourceLog.impression_count.name(), 0);
            db.insertOrThrow(SourceLog.TABLE_NAME, null, cv);
        }
    }

    private void logImpressions(List<SuggestionPosition> impressions, long now) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        final ContentValues cv = new ContentValues();
        try {
            // TODO: aggregate the impressions and insert one row per source
            for (SuggestionPosition suggestionPos : impressions) {
                SuggestionCursor suggestion = suggestionPos.getSuggestion();
                ComponentName name = suggestion.getSourceComponentName();

                cv.put(SourceLog.component.name(), name.flattenToString());
                cv.put(SourceLog.time.name(), now);
                cv.put(SourceLog.click_count.name(), 0);
                cv.put(SourceLog.impression_count.name(), 1);
                db.insertOrThrow(SourceLog.TABLE_NAME, null, cv);

                cv.clear();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        postSourceEventCleanup(now);
    }

    /**
     * Execute queries necessary to keep things up to date after inserting into {@link SourceLog}.
     *
     * Note: we aren't using a TRIGGER because there are usually several writes to the log at a
     * time, and triggers execute on each individual row insert.
     *
     * @param now Millis since epoch of "now".
     */
    private void postSourceEventCleanup(long now) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // purge old log entries
        db.execSQL("DELETE FROM " + SourceLog.TABLE_NAME + " WHERE "
                + SourceLog.time.name() + " <"
                + now + " - " + mConfig.getMaxSourceEventAgeMillis() + ";");

        // update the source stats
        final String columns = SourceLog.component + "," +
                "SUM(" + SourceLog.click_count.fullName + ")" + "," +
                "SUM(" + SourceLog.impression_count.fullName + ")";
        db.execSQL("DELETE FROM " + SourceStats.TABLE_NAME);
        db.execSQL("INSERT INTO " + SourceStats.TABLE_NAME  + " "
                + "SELECT " + columns + " FROM " + SourceLog.TABLE_NAME + " GROUP BY "
                + SourceLog.component.name());
    }

// -------------------------- TABLES --------------------------

    /**
     * shortcuts table
     */
    enum Shortcuts {
        intent_key,
        source,
        format,
        title,
        description,
        icon1,
        icon2,
        intent_action,
        intent_data,
        intent_query,
        intent_extradata,
        shortcut_id;

        static final String TABLE_NAME = "shortcuts";

        public final String fullName;

        Shortcuts() {
            fullName = TABLE_NAME + "." + name();
        }
    }

    /**
     * clicklog table. Has one record for each click.
     */
    enum ClickLog {
        _id,
        intent_key,
        query,
        hit_time;

        static final String[] COLUMNS = initColumns();

        static final String TABLE_NAME = "clicklog";

        private static String[] initColumns() {
            ClickLog[] vals = ClickLog.values();
            String[] columns = new String[vals.length];
            for (int i = 0; i < vals.length; i++) {
                columns[i] = vals[i].fullName;
            }
            return columns;
        }

        public final String fullName;

        ClickLog() {
            fullName = TABLE_NAME + "." + name();
        }
    }

    /**
     * We store stats about clicks and impressions per source to facilitate the ranking of
     * the sources, and which are promoted vs under the "more results" entry.
     */
    enum SourceLog {
        _id,
        component,
        time,
        click_count,
        impression_count;

        static final String[] COLUMNS = initColumns();

        static final String TABLE_NAME = "sourceeventlog";

        private static String[] initColumns() {
            SourceLog[] vals = SourceLog.values();
            String[] columns = new String[vals.length];
            for (int i = 0; i < vals.length; i++) {
                columns[i] = vals[i].fullName;
            }
            return columns;
        }

        public final String fullName;

        SourceLog() {
            fullName = TABLE_NAME + "." + name();
        }
    }

    /**
     * This is an aggregate table of {@link SourceLog} that stays up to date with the total
     * clicks and impressions for each source.  This makes computing the source ranking more
     * more efficient, at the expense of some extra work when the source clicks and impressions
     * are reported at the end of the session.
     */
    enum SourceStats {
        component,
        total_clicks,
        total_impressions;

        static final String TABLE_NAME = "sourcetotals";

        static final String[] COLUMNS = initColumns();

        private static String[] initColumns() {
            SourceStats[] vals = SourceStats.values();
            String[] columns = new String[vals.length];
            for (int i = 0; i < vals.length; i++) {
                columns[i] = vals[i].fullName;
            }
            return columns;
        }

        public final String fullName;

        SourceStats() {
            fullName = TABLE_NAME + "." + name();
        }
    }

// -------------------------- END TABLES --------------------------

    // contains creation and update logic
    private static class DbOpenHelper extends SQLiteOpenHelper {
        private Config mConfig;
        private String mPath;
        private static final String SHORTCUT_ID_INDEX
                = Shortcuts.TABLE_NAME + "_" + Shortcuts.shortcut_id.name();
        private static final String CLICKLOG_QUERY_INDEX
                = ClickLog.TABLE_NAME + "_" + ClickLog.query.name();
        private static final String CLICKLOG_HIT_TIME_INDEX
                = ClickLog.TABLE_NAME + "_" + ClickLog.hit_time.name();
        private static final String CLICKLOG_PURGE_TRIGGER
                = ClickLog.TABLE_NAME + "_purge";
        private static final String SHORTCUTS_DELETE_TRIGGER
                = Shortcuts.TABLE_NAME + "_delete";
        private static final String SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER
                = Shortcuts.TABLE_NAME + "_update_intent_key";

        public DbOpenHelper(Context context, String name, int version, Config config) {
            super(context, name, null, version);
            mConfig = config;
        }

        public String getPath() {
            return mPath;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // The shortcuts info is not all that important, so we just drop the tables
            // and re-create empty ones.
            Log.i(TAG, "Upgrading shortcuts DB from version " +
                    + oldVersion + " to " + newVersion + ". This deletes all shortcuts.");
            dropTables(db);
            onCreate(db);
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TRIGGER IF EXISTS " + CLICKLOG_PURGE_TRIGGER);
            db.execSQL("DROP TRIGGER IF EXISTS " + SHORTCUTS_DELETE_TRIGGER);
            db.execSQL("DROP TRIGGER IF EXISTS " + SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER);
            db.execSQL("DROP INDEX IF EXISTS " + CLICKLOG_HIT_TIME_INDEX);
            db.execSQL("DROP INDEX IF EXISTS " + CLICKLOG_QUERY_INDEX);
            db.execSQL("DROP INDEX IF EXISTS " + SHORTCUT_ID_INDEX);
            db.execSQL("DROP TABLE IF EXISTS " + ClickLog.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Shortcuts.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + SourceLog.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + SourceStats.TABLE_NAME);
        }

        private void clearDatabase(SQLiteDatabase db) {
            db.delete(ClickLog.TABLE_NAME, null, null);
            db.delete(Shortcuts.TABLE_NAME, null, null);
            db.delete(SourceLog.TABLE_NAME, null, null);
            db.delete(SourceStats.TABLE_NAME, null, null);
        }

        /**
         * Deletes the database file.
         */
        public void deleteDatabase() {
            close();
            if (mPath == null) return;
            try {
                new File(mPath).delete();
                if (DBG) Log.d(TAG, "deleted " + mPath);
            } catch (Exception e) {
                Log.w(TAG, "couldn't delete " + mPath, e);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            mPath = db.getPath();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Shortcuts.TABLE_NAME + " (" +
                    // COLLATE UNICODE is needed to make it possible to use nextString()
                    // to implement fast prefix filtering.
                    Shortcuts.intent_key.name() + " TEXT NOT NULL COLLATE UNICODE PRIMARY KEY, " +
                    Shortcuts.source.name() + " TEXT NOT NULL, " +
                    Shortcuts.format.name() + " TEXT, " +
                    Shortcuts.title.name() + " TEXT, " +
                    Shortcuts.description.name() + " TEXT, " +
                    Shortcuts.icon1.name() + " TEXT, " +
                    Shortcuts.icon2.name() + " TEXT, " +
                    Shortcuts.intent_action.name() + " TEXT, " +
                    Shortcuts.intent_data.name() + " TEXT, " +
                    Shortcuts.intent_query.name() + " TEXT, " +
                    Shortcuts.intent_extradata.name() + " TEXT, " +
                    Shortcuts.shortcut_id.name() + " TEXT" +
                    ");");

            // index for fast lookup of shortcuts by shortcut_id
            db.execSQL("CREATE INDEX " + SHORTCUT_ID_INDEX
                    + " ON " + Shortcuts.TABLE_NAME
                    + "(" + Shortcuts.shortcut_id.name() + ", " + Shortcuts.source.name() + ")");

            db.execSQL("CREATE TABLE " + ClickLog.TABLE_NAME + " ( " +
                    ClickLog._id.name() + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    // type must match Shortcuts.intent_key
                    ClickLog.intent_key.name() + " TEXT NOT NULL COLLATE UNICODE REFERENCES "
                        + Shortcuts.TABLE_NAME + "(" + Shortcuts.intent_key + "), " +
                    ClickLog.query.name() + " TEXT, " +
                    ClickLog.hit_time.name() + " INTEGER" +
                    ");");

            // index for fast lookup of clicks by query
            db.execSQL("CREATE INDEX " + CLICKLOG_QUERY_INDEX
                    + " ON " + ClickLog.TABLE_NAME + "(" + ClickLog.query.name() + ")");

            // index for finding old clicks quickly
            db.execSQL("CREATE INDEX " + CLICKLOG_HIT_TIME_INDEX
                    + " ON " + ClickLog.TABLE_NAME + "(" + ClickLog.hit_time.name() + ")");

            // trigger for purging old clicks, i.e. those such that
            // hit_time < now - MAX_MAX_STAT_AGE_MILLIS, where now is the
            // hit_time of the inserted record
            db.execSQL("CREATE TRIGGER " + CLICKLOG_PURGE_TRIGGER + " AFTER INSERT ON "
                    + ClickLog.TABLE_NAME
                    + " BEGIN"
                    + " DELETE FROM " + ClickLog.TABLE_NAME + " WHERE "
                            + ClickLog.hit_time.name() + " <"
                            + " NEW." + ClickLog.hit_time.name()
                                    + " - " + mConfig.getMaxStatAgeMillis() + ";"
                    + " END");

            // trigger for deleting clicks about a shortcut once that shortcut has been
            // deleted
            db.execSQL("CREATE TRIGGER " + SHORTCUTS_DELETE_TRIGGER + " AFTER DELETE ON "
                    + Shortcuts.TABLE_NAME
                    + " BEGIN"
                    + " DELETE FROM " + ClickLog.TABLE_NAME + " WHERE "
                            + ClickLog.intent_key.name()
                            + " = OLD." + Shortcuts.intent_key.name() + ";"
                    + " END");

            // trigger for updating click log entries when a shortcut changes its intent_key
            db.execSQL("CREATE TRIGGER " + SHORTCUTS_UPDATE_INTENT_KEY_TRIGGER
                    + " AFTER UPDATE ON " + Shortcuts.TABLE_NAME
                    + " WHEN NEW." + Shortcuts.intent_key.name()
                            + " != OLD." + Shortcuts.intent_key.name()
                    + " BEGIN"
                    + " UPDATE " + ClickLog.TABLE_NAME + " SET "
                            + ClickLog.intent_key.name() + " = NEW." + Shortcuts.intent_key.name()
                            + " WHERE "
                            + ClickLog.intent_key.name() + " = OLD." + Shortcuts.intent_key.name()
                            + ";"
                    + " END");

            db.execSQL("CREATE TABLE " + SourceLog.TABLE_NAME + " ( " +
                    SourceLog._id.name() + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    SourceLog.component.name() + " TEXT NOT NULL COLLATE UNICODE, " +
                    SourceLog.time.name() + " INTEGER, " +
                    SourceLog.click_count + " INTEGER, " +
                    SourceLog.impression_count + " INTEGER);"
            );

            db.execSQL("CREATE TABLE " + SourceStats.TABLE_NAME + " ( " +
                    SourceStats.component.name() + " TEXT NOT NULL COLLATE UNICODE PRIMARY KEY, " +
                    SourceStats.total_clicks + " INTEGER, " +
                    SourceStats.total_impressions + " INTEGER);"
                    );
        }
    }
}
