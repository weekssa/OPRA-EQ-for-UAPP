package com.weekssa.opraeqforuapp

import android.app.Application
import androidx.work.WorkManager
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.data.managed.QaReviewSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Temporary QA-only application that seeds one pending updated-EQ review before MainActivity opens. */
class QaReviewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).cancelAllWork()
        runBlocking(Dispatchers.IO) {
            val database = OpraEqDatabase.create(this@QaReviewApplication)
            try {
                QaReviewSeeder(database).seedIfMissing()
            } finally {
                database.close()
            }
        }
    }
}
