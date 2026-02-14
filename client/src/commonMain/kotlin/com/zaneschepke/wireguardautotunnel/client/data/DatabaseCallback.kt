package com.zaneschepke.wireguardautotunnel.client.data

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class DatabaseCallback(private val databaseProvider: Lazy<AppDatabase>) : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        super.onCreate(connection)
        connection.execSQL("INSERT INTO lockdown_settings DEFAULT VALUES")
        connection.execSQL("INSERT INTO general_settings DEFAULT VALUES")
    }
}
