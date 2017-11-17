package com.wijdemans.songunscrambler

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = DbHelper(this)

        btnUnscramble.setOnClickListener {
            AsyncLookup().execute()
        }
    }

    inner class AsyncLookup : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg p0: String?): String {
            val inputText = txtScrambled.text.toString()
            Log.v(TAG, "searching for ${inputText}")
            val result = dbHelper.find(inputText)
            return result
        }

        override fun onPostExecute(result: String?) {
            txtResults.setText(result)
        }
    }

}
