package com.yigit.prodex

import android.app.Application
import com.yigit.prodex.data.AppDatabase
import com.yigit.prodex.util.SessionManager

class ProdExApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sessionManager: SessionManager by lazy { SessionManager(this) }
}
