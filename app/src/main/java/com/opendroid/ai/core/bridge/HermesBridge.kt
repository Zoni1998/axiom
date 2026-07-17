package com.opendroid.ai.core.bridge

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.opendroid.ai.actions.ActionDispatcher
import com.opendroid.ai.actions.base.ActionResult
import com.opendroid.ai.data.models.ChatMessage
import com.opendroid.ai.data.repository.ConversationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HermesBridge — conecta o Hermes (cérebro no servidor) ao ZonIA (corpo no Android).
 * 
 * Formato de comando (via Telegram):
 *   🤖ZONIA|OPEN_APP|appName=WhatsApp
 *   🤖ZONIA|SEND_WHATSAPP|contact=Victor|message=Oi
 *   🤖ZONIA|SET_BRIGHTNESS|level=80
 *   🤖ZONIA|TOGGLE_FLASHLIGHT|state=on
 *   🤖ZONIA|TAKE_SCREENSHOT
 *   🤖ZONIA|GET_LOCATION
 *   🤖ZONIA|CHAT|text=Sua mensagem aqui
 * 
 * Resposta automática enviada de volta como notificação.
 */
@Singleton
class HermesBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val actionDispatcher: dagger.Lazy<ActionDispatcher>,
    private val conversationRepository: ConversationRepository
) {
    companion object {
        private const val TAG = "HermesBridge"
        const val COMMAND_PREFIX = "🤖ZONIA|"
        const val RESPONSE_PREFIX = "✅ZONIA|"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Check if a Telegram notification contains a Hermes command.
     * Called from the notification listener.
     */
    fun checkAndExecute(notification: Notification, packageName: String) {
        if (packageName != "org.telegram.messenger" && 
            packageName != "org.telegram.messenger.web" &&
            packageName != "org.telegram.messenger.beta") return

        val text = extractNotificationText(notification) ?: return
        if (!text.contains(COMMAND_PREFIX)) return

        // Extract the command part
        val commandPart = text.substringAfter(COMMAND_PREFIX).substringBefore("\n").trim()
        if (commandPart.isBlank()) return

        scope.launch {
            executeCommand(commandPart)
        }
    }

    private suspend fun executeCommand(commandPart: String) {
        val parts = commandPart.split("|")
        val action = parts.getOrNull(0)?.trim()?.uppercase() ?: return

        if (action == "CHAT") {
            // Direct chat message — insert into conversation
            val chatText = commandPart.substringAfter("text=").trimStart('=')
            if (chatText.isNotBlank()) {
                val msg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = chatText,
                    sender = ChatMessage.Sender.AGENT,
                    modelBadge = "Hermes"
                )
                conversationRepository.insertMessage(msg)
            }
            return
        }

        // Parse params
        val paramsStr = parts.drop(1).joinToString("|")
        val params = mutableMapOf<String, String>()
        if (paramsStr.isNotBlank()) {
            paramsStr.split("|").forEach { pair ->
                val keyVal = pair.split("=", limit = 2)
                if (keyVal.size == 2) {
                    params[keyVal[0].trim()] = keyVal[1].trim()
                }
            }
        }

        try {
            val dispatcher = actionDispatcher.get()
            val result = dispatcher.execute(action, params, context)

            // Report back via conversation + log
            val resultText = when {
                result.success -> "✅ ${result.data ?: "OK"}"
                result is ActionResult.NeedsInput -> "❓ ${result.question}"
                else -> "❌ ${result.error ?: "Falhou"}"
            }

            val responseMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "[Hermes] $action → $resultText",
                sender = ChatMessage.Sender.AGENT,
                modelBadge = "Bridge"
            )
            conversationRepository.insertMessage(responseMsg)
            Log.i(TAG, "Command executed: $action → $resultText")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute $action: ${e.message}", e)
        }
    }

    /**
     * Extract text content from a notification.
     */
    private fun extractNotificationText(notification: Notification): String? {
        val extras = notification.extras
        val sb = StringBuilder()

        // Try all possible text sources
        val sources = listOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
            extras.getCharSequence("android.text"),
            extras.getCharSequence("android.bigText"),
            notification.tickerText
        )

        for (source in sources) {
            if (source != null && source.isNotBlank()) {
                sb.appendLine(source)
            }
        }

        // Try messaging-style messages (Telegram uses this)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null) {
            for (msg in messages) {
                if (msg is Bundle) {
                    val sender = msg.getCharSequence("sender")
                    val text = msg.getCharSequence("text")
                    if (sender != null) sb.append("$sender: ")
                    if (text != null) sb.appendLine(text)
                } else {
                    // Fallback: toString
                    sb.appendLine(msg.toString())
                }
            }
        }

        // Also try CharSequence[] lines
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null) {
            for (line in textLines) {
                if (line != null) sb.appendLine(line)
            }
        }

        val result = sb.toString().trim()
        if (result.isNotBlank()) {
            Log.d(TAG, "Extracted notification text (${result.length} chars): ${result.take(200)}")
        }
        return result.takeIf { it.isNotBlank() }
    }
}
