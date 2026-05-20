package com.prexoft.replygpt

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReplyService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)
    private var model: GenerativeModel? = null
    private val repliedMessages = mutableSetOf<String>()
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        updateConfig()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun updateConfig() {
        val apiKey = "<<hidden>>"
        if (apiKey.isNotBlank()) {
            val userInfo = prefs.getUserInfo()
            val systemPrompt = """
                You're the personal assistant of this device that generates replies to incoming notifications (all the further inputs which will be given to you will be notification content)
                 
                About devive owner:
                $userInfo
                
                Your goals:
                Respond in a way that fits human conversation, be funny and sarcastic.
                If clarification is need, ask for it.
                Adapt the style and language to match sender's message.
                Never share the information of one sender to other.
                Do not repeat incoming message or give explanations in your reply.
                
                Input format:
                App: <app name>
                Sender: <sender name>
                Message: <message>
                Time: <time>
            """.trimIndent()

            model = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = apiKey,
                systemInstruction = content { text(systemPrompt) }
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (sbn.isOngoing) return

        val packageName = sbn.packageName
        if (!PreferencesManager(this).getEnabledPackages().contains(packageName)) return

        val notification = sbn.notification
        val extras = notification.extras ?: return
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getString(Notification.EXTRA_TEXT) ?: return

        if (sender.equals("You", ignoreCase = true)) return

        val actions = notification.actions ?: return
        val messageId = "$packageName|$sender|$text"

        if (repliedMessages.contains(messageId)) return

        for (action in actions) {
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                repliedMessages.add(messageId)
                if (repliedMessages.size > 100) repliedMessages.clear()

                generateReply(action, action.remoteInputs, packageName, sender, text)
                break
            }
        }
    }

    private fun generateReply(
        action: Notification.Action,
        remoteInputs: Array<RemoteInput>,
        packageName: String,
        sender: String,
        text: String
    ) {
        if (model == null) {
            updateConfig()
            if (model == null) return
        }

        serviceScope.launch {
            try {
                val prompt = """
                    App: $packageName
                    Sender: $sender
                    Message: $text
                    Time: ${formatDate(System.currentTimeMillis())}
                """.trimIndent()

                val response = model?.generateContent(prompt)
                val replyText = response?.text?.trim()

                if (!replyText.isNullOrEmpty()) {
                    sendQuickReply(action.actionIntent, remoteInputs, replyText)
                    prefs.incrementDailyReplyCount()
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendQuickReply(
        pendingIntent: PendingIntent,
        remoteInputs: Array<RemoteInput>,
        replyText: String
    ) {
        val intent = Intent()
        val bundle = Bundle()

        for (remoteInput in remoteInputs) {
            bundle.putCharSequence(remoteInput.resultKey, replyText)
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

        try {
            pendingIntent.send(this, 0, intent)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}