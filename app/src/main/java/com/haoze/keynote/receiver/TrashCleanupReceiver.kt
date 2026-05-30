package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrashCleanupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NoteDatabase.getDatabase(context)
                val expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                db.noteDao().deleteExpiredTrashNotes(expireTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
