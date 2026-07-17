package com.opendroid.ai.core.agent

import com.opendroid.ai.core.llm.LLMProviderFactory
import com.opendroid.ai.core.llm.LLMRequest
import com.opendroid.ai.core.llm.ResponseFormat
import com.opendroid.ai.data.models.ChatMessage
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

enum class QueryComplexity {
    SIMPLE, MEDIUM, COMPLEX
}

@Singleton
class IntentClassifier @Inject constructor(
    private val llmProviderFactory: dagger.Lazy<LLMProviderFactory>
) {
    suspend fun requiresAction(query: String): Boolean {
        // Aggressive keyword matching — skip LLM, always route actions correctly
        val lower = query.lowercase()
        
        // PT-BR action patterns — most common
        val ptPatterns = listOf(
            "abrir", "abre", "abra", "aberto", "abrindo",
            "ligar", "liga", "ligue", "ligado", "ligando",
            "desligar", "desliga", "desligue",
            "enviar", "envia", "envie", "manda", "mandar", "mande",
            "aumentar", "aumenta", "aumente", "diminuir", "diminui", "diminua",
            "tirar", "tira", "tire", "tirando",
            "tocar", "toca", "toque", "tocando",
            "mostrar", "mostra", "mostre", "exibir", "exibe", "exiba",
            "calcular", "calcula", "calcule", "calculando",
            "pesquisar", "pesquisa", "pesquise", "buscar", "busca", "busque",
            "traduzir", "traduz", "traduza",
            "copiar", "copia", "copie", "colar", "cola", "cole",
            "bloquear", "bloqueia", "bloqueie", "travar", "trava", "trave",
            "silenciar", "silencia", "mutar", "muta",
            "acender", "acende", "apagar", "apaga",
            "gravar", "grava", "grave", "filmar", "filma", "filme",
        )
        
        // EN action patterns  
        val enPatterns = listOf(
            "open", "launch", "start", "turn", "toggle", "enable", "disable",
            "set", "lock", "restart", "take", "record", "send", "make",
            "play", "pause", "resume", "order", "search", "pay", "check",
            "split", "run", "create", "schedule", "read", "write", "delete",
            "click", "type", "scroll", "get", "show",
        )
        
        // App/service names that imply actions
        val appNames = listOf(
            "whatsapp", "zap", "telegram", "instagram", "facebook", "twitter",
            "youtube", "spotify", "netflix", "chrome", "firefox", "navegador",
            "mapas", "maps", "google maps", "waze", "uber", "câmera", "camera",
            "galeria", "gallery", "calculadora", "bloco de notas", "notepad",
            "configurações", "settings", "ajustes"
        )
        
        // Target objects that imply actions
        val targetObjects = listOf(
            "lanterna", "flashlight", "flash", "torch", "brilho", "brightness",
            "volume", "som", "áudio", "audio", "wifi", "bluetooth", "dados móveis",
            "hotspot", "não perturbe", "dnd", "alarme", "alarm", "timer",
            "lembrete", "reminder", "print", "screenshot", "captura", "tela",
            "notificação", "notification", "mensagem", "message", "mensagens",
            "foto", "photo", "fotos", "photos", "vídeo", "video"
        )
        
        val hasPtAction = ptPatterns.any { lower.contains(it) }
        val hasEnAction = enPatterns.any { lower.contains(it) }
        val hasApp = appNames.any { lower.contains(it) }
        val hasTarget = targetObjects.any { lower.contains(it) }
        
        // If the query has an action verb + target/app, it's an action
        if ((hasPtAction || hasEnAction) && (hasApp || hasTarget)) return true
        // If it has both PT and EN markers, likely an action
        if (hasPtAction && hasEnAction) return true
        // If it mentions an app with imperative tone, it's an action
        if (hasApp && (lower.contains("me") || lower.contains("pra") || lower.contains("para"))) return true
        
        // Default: if the query is short and looks like a command, treat as action
        if (lower.length < 50 && (hasPtAction || hasEnAction || hasApp || hasTarget)) return true
        
        // Only pure conversation goes to chat mode
        return false
    }

    fun classifyComplexity(query: String): QueryComplexity {
        val lowercaseQuery = query.lowercase()

        // ── Fast-path: known single-intent patterns that happen to contain
        //    multiple action keywords (e.g. "set" + "brightness").
        //    These must be classified as SIMPLE so the AliasResolver handles them.
        val singleIntentPatterns = listOf(
            "set brightness", "set volume", "set alarm", "set timer", "set reminder",
            "turn on flashlight", "turn off flashlight", "turn on wifi", "turn off wifi",
            "turn on bluetooth", "turn off bluetooth", "turn on hotspot", "turn off hotspot",
            "turn on dnd", "turn off dnd", "turn on torch", "turn off torch",
            "take screenshot", "take a screenshot", "take ss",
            "enable wifi", "disable wifi", "enable bluetooth", "disable bluetooth",
            "enable hotspot", "disable hotspot", "enable dnd", "disable dnd",
            "toggle flashlight", "toggle wifi", "toggle bluetooth", "toggle hotspot", "toggle dnd",
            "open settings", "open camera", "lock screen", "lock phone",
            "play music", "pause music", "resume music", "next song", "previous song",
            "mute phone", "unmute phone", "set wallpaper",
            // PT-BR
            "aumenta o brilho", "aumentar brilho", "diminui o brilho", "diminuir brilho",
            "liga a lanterna", "ligar lanterna", "desliga a lanterna", "desligar lanterna",
            "tira um print", "tirar print", "capturar tela",
            "aumenta o volume", "aumentar volume", "diminui o volume", "diminuir volume",
            "liga o wifi", "ligar wifi", "desliga o wifi", "desligar wifi",
            "abrir whatsapp", "abrir zap", "abrir maps", "abrir camera", "abrir câmera",
            "abrir configurações", "abrir ajustes", "abrir chrome", "abrir youtube",
            "modo silencioso", "modo vibrar", "não perturbe", "bloquear tela"
        )
        if (singleIntentPatterns.any { lowercaseQuery.startsWith(it) || lowercaseQuery == it }) {
            return QueryComplexity.SIMPLE
        }

        // Check for compound indicators FIRST — these always indicate multi-step tasks
        val compoundIndicators = listOf(
            " and then ", " then ", " after that ", " after ",
            " also ", " plus ", "and send", "and message",
            "and call", "and tell", "and notify", "and text",
            "and book", "and set", "and create", "and open",
            "then send", "then call", "then message",
            "then open", "then set", "then play",
            "and also ", "followed by", "afterwards"
        )

        val isCompound = compoundIndicators.any { lowercaseQuery.contains(it) }

        // Additionally check for " and " with action verbs on both sides
        val andWithActions = hasActionsAroundAnd(lowercaseQuery)

        if (isCompound || andWithActions) {
            // Compound tasks are ALWAYS at least MEDIUM
            // Check if it's complex (3+ actions)
            val compoundCount = compoundIndicators.count { lowercaseQuery.contains(it) }
            return if (compoundCount >= 2) QueryComplexity.COMPLEX else QueryComplexity.MEDIUM
        }

        // ONLY then check for simple patterns using original logic
        val actionTriggers = listOf(
            "open", "launch", "start", "turn", "toggle", "enable", "disable", "set", "lock", "restart",
            "take", "record", "send", "make", "play", "pause", "resume", "next", "prev", "order", "search",
            "pay", "check", "split", "run", "create", "schedule", "list", "read", "write", "delete", "click",
            "type", "scroll", "get", "show", "whatsapp", "call", "sms", "email", "alarm", "timer", "reminder",
            "note", "notes", "calendar", "weather", "news", "flashlight", "flash", "wifi", "bluetooth",
            "brightness", "volume", "screenshot", "dnd", "mute", "unmute"
        )
        
        val sequenceConjunctions = listOf(
            "and then", "then", "after that", "next", "also", "followed by", "afterwards", "later"
        )
        
        // Check for multiple commands separated by punctuation
        val commandSeparators = Pattern.compile("[,.;]")
        val matcher = commandSeparators.matcher(query)
        var separatorCount = 0
        while (matcher.find()) {
            separatorCount++
        }

        val triggerCount = actionTriggers.count { lowercaseQuery.contains(it) }
        val conjunctionCount = sequenceConjunctions.count { lowercaseQuery.contains(it) }
        
        val totalComplexityScore = triggerCount + conjunctionCount + (separatorCount * 0.5)

        return when {
            totalComplexityScore >= 4.0 || lowercaseQuery.contains("loop") || lowercaseQuery.contains("repeat") -> QueryComplexity.COMPLEX
            totalComplexityScore >= 2.0 -> QueryComplexity.MEDIUM
            else -> QueryComplexity.SIMPLE
        }
    }

    private fun hasActionsAroundAnd(query: String): Boolean {
        // Detect patterns like "open X and send Y" where "and" connects two action verbs
        val actionVerbs = listOf(
            "open", "send", "call", "message", "set", "play", "book",
            "create", "make", "turn", "toggle", "take", "record",
            "search", "order", "pay", "check", "read", "write"
        )
        val andIndex = query.indexOf(" and ")
        if (andIndex == -1) return false

        val before = query.substring(0, andIndex)
        val after = query.substring(andIndex + 5)

        val hasActionBefore = actionVerbs.any { before.contains(it) }
        val hasActionAfter = actionVerbs.any { after.contains(it) }

        return hasActionBefore && hasActionAfter
    }
}
