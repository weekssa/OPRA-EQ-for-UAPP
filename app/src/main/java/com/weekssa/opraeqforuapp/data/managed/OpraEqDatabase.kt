package com.weekssa.opraeqforuapp.data.managed

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ManagedHeadphoneEntity::class, ManagedProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OpraEqDatabase : RoomDatabase() {
    abstract fun managedHeadphonesDao(): ManagedHeadphonesDao

    companion object {
        fun create(context: Context): OpraEqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpraEqDatabase::class.java,
                "opra_eq_for_uapp.db",
            ).build()
    }
}
