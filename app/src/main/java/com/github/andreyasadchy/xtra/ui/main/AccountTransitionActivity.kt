package com.github.andreyasadchy.xtra.ui.main

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.github.andreyasadchy.xtra.util.PrototypeAccounts

/** A separate process guarantees old chat, player, worker and repository state is gone. */
class AccountTransitionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oldPid = intent.getIntExtra("old_pid", -1)
        if (oldPid > 0 && oldPid != Process.myPid()) Process.killProcess(oldPid)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                PrototypeAccounts.activate(this, intent.getStringExtra("account"))
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("prototype_add_account", intent.getBooleanExtra("add", false))
                    putExtra("prototype_previous_account", intent.getStringExtra("previous"))
                })
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open saved account. Please sign in again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            finish()
            Process.killProcess(Process.myPid())
        }, 500)
    }
}
