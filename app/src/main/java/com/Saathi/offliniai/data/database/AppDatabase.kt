package com.Saathi.offliniai.data.database

import android.content.Context

object AppDatabase {
    fun getInstance(context: Context): ChatDatabase = ChatDatabase.getInstance(context)
}
