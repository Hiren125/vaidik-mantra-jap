package com.vedicapps.mantrajap

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context, private val db: AppDatabase) {

    suspend fun syncFixedMantras() {
        try {
            val dbUrl = "https://vedicmantrajap-native-default-rtdb.asia-southeast1.firebasedatabase.app/"
            val database = FirebaseDatabase.getInstance(dbUrl).getReference("fixed_mantras")

            val snapshot = database.get().await()

            if (snapshot.exists()) {
                withContext(Dispatchers.IO) {
                    // 1. WIPE local suggested mantras first to handle cloud deletions
                    db.mantraDao().deleteFixedMantras()

                    // 2. Loop through what is currently on Firebase
                    for (child in snapshot.children) {
                        // Automatically map Firebase data to your Mantra class
                        val mantra = child.getValue(Mantra::class.java)

                        mantra?.let {
                            it.id = 0L // Force Room to generate a new local ID
                            it.remoteKey = child.key ?: ""
                            it.isFixed = true

                            // 3. Insert fresh data
                            db.mantraDao().insertMantra(it)
                        }
                    }
                }
            } else {
                // If cloud is empty, local suggested list should also be empty
                withContext(Dispatchers.IO) {
                    db.mantraDao().deleteFixedMantras()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // Toast.makeText(context, "Sync Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}