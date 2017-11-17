package com.wijdemans.songunscrambler

import android.content.Context
import android.database.DatabaseUtils
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

// SQLiteAssetHelper also takes care of copying the database for the apk to the device
class DbHelper(context: Context) : SQLiteAssetHelper(context, "songs.db", null, 2) {

    private val TAG = DbHelper::class.java.simpleName

    fun find(scramble: String): String {
        Log.v(TAG, "Searching for $scramble")
        Log.i(TAG, "Found ${DatabaseUtils.queryNumEntries(this.readableDatabase, "songs")} songs...")

        val c = this.readableDatabase.query("songs", arrayOf("artist", "song"),
                "scramble=?", arrayOf(scramble),
                null, null, null, "10")

        c.use { c ->
            while (c.moveToNext()) {
                return c.getString(0) + " - " + c.getString(1)
            }
        }
        return "n/a"
    }


}
