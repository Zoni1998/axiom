package com.opendroid.ai.ui.screens

import android.Manifest
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.opendroid.ai.ui.theme.*

enum class OnboardingStage {
    INTRODUCTION,
    PERMISSION_PROMPT,
    PERMISSIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { com.opendroid.ai.core.security.SecurePrefs.get(context) }

    var stage by remember { mutableStateOf(OnboardingStage.INTRODUCTION) }
    var name by remember { mutableStateOf(sharedPrefs.getString("user_name", "") ?: "") }
    var dob by remember { mutableStateOf(sharedPrefs.getString("user_dob", "") ?: "") }
    var showError by remember { mutableStateOf(false) }

    // Core permissions status state
    var recordAudioGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.RECORD_AUDIO)) }
    var locationGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var smsGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.SEND_SMS)) }
    var phoneGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.CALL_PHONE)) }
    var contactsGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.READ_CONTACTS)) }
    var calendarGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.READ_CALENDAR)) }
    var cameraGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.CAMERA)) }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPerm(context, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }
        )
    }
    var storageGranted by remember { mutableStateOf(hasStoragePermission(context)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var writeSettingsGranted by remember { mutableStateOf(Settings.System.canWrite(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = isAccessibilityServiceEnabled(context)
                recordAudioGranted = checkPerm(context, Manifest.permission.RECORD_AUDIO)
                locationGranted = checkPerm(context, Manifest.permission.ACCESS_FINE_LOCATION)
                smsGranted = checkPerm(context, Manifest.permission.SEND_SMS)
                phoneGranted = checkPerm(context, Manifest.permission.CALL_PHONE)
                contactsGranted = checkPerm(context, Manifest.permission.READ_CONTACTS)
                calendarGranted = checkPerm(context, Manifest.permission.READ_CALENDAR)
                cameraGranted = checkPerm(context, Manifest.permission.CAMERA)
                notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkPerm(context, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true
                }
                storageGranted = hasStoragePermission(context)
                writeSettingsGranted = Settings.System.canWrite(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        recordAudioGranted = it
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        locationGranted = it
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        smsGranted = it
    }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        phoneGranted = it
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsGranted = it
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        calendarGranted = it
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }
    val legacyStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when (stage) {
                        OnboardingStage.INTRODUCTION -> "Sobre Você"
                        OnboardingStage.PERMISSION_PROMPT -> "Permissões"
                        OnboardingStage.PERMISSIONS -> "Conceder Permissões"
                    }
                    Text(titleText, color = AccentSecondary, fontWeight = FontWeight.Bold) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        when (stage) {
            OnboardingStage.INTRODUCTION -> {
                IntroductionPanel(
                    name = name,
                    onNameChange = { name = it; showError = false },
                    dob = dob,
                    onDobChange = { dob = it; showError = false },
                    showError = showError,
                    onContinue = {
                        if (name.isBlank() || dob.isBlank()) {
                            showError = true
                        } else {
                            sharedPrefs.edit()
                                .putString("user_name", name)
                                .putString("user_dob", dob)
                                .apply()
                            stage = OnboardingStage.PERMISSION_PROMPT
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            OnboardingStage.PERMISSION_PROMPT -> {
                PermissionPromptPanel(
                    onContinue = {
                        stage = OnboardingStage.PERMISSIONS
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            OnboardingStage.PERMISSIONS -> {
                PermissionsPanelContent(
                    padding = padding,
                    recordAudioGranted = recordAudioGranted,
                    locationGranted = locationGranted,
                    smsGranted = smsGranted,
                    phoneGranted = phoneGranted,
                    contactsGranted = contactsGranted,
                    calendarGranted = calendarGranted,
                    cameraGranted = cameraGranted,
                    notificationsGranted = notificationsGranted,
                    storageGranted = storageGranted,
                    accessibilityGranted = accessibilityGranted,
                    writeSettingsGranted = writeSettingsGranted,
                    onAudioGrant = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onLocationGrant = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    onSmsPhoneGrant = {
                        smsLauncher.launch(Manifest.permission.SEND_SMS)
                        phoneLauncher.launch(Manifest.permission.CALL_PHONE)
                    },
                    onContactsCalendarGrant = {
                        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onCameraGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    onNotificationsGrant = { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onWriteSettingsGrant = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    onStorageGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        } else {
                            legacyStorageLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        }
                    },
                    onAccessibilityGrant = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                    onFinished = {
                        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
                        onFinished()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroductionPanel(
    name: String,
    onNameChange: (String) -> Unit,
    dob: String,
    onDobChange: (String) -> Unit,
    showError: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(3.dp, Brush.horizontalGradient(listOf(AccentSecondary, AccentCyan)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.opendroid.ai.R.drawable.bot),
                contentDescription = "ZonIA Avatar",
                modifier = Modifier.size(120.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Olá! Eu sou o ZonIA",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Seu assistente autônomo open source. Me conte sobre você para que eu possa te ajudar melhor.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Como devo te chamar?", color = TextSecondary) },
            placeholder = { Text("Digite seu nome", color = TextSecondary.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentSecondary,
                unfocusedBorderColor = BorderColor,
                focusedLabelColor = AccentSecondary,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentSecondary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dob,
            onValueChange = onDobChange,
            label = { Text("Qual sua data de nascimento?", color = TextSecondary) },
            placeholder = { Text("Ex: DD/MM/AAAA", color = TextSecondary.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentSecondary,
                unfocusedBorderColor = BorderColor,
                focusedLabelColor = AccentSecondary,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentSecondary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onContinue() }),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Por favor, preencha nome e data de nascimento.",
                color = AccentRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary, contentColor = DarkBackground),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Vamos Lá", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PermissionPromptPanel(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(3.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentPrimary)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.opendroid.ai.R.drawable.bot),
                contentDescription = "ZonIA Avatar",
                modifier = Modifier.size(120.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Configurar Permissões",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Me dê as permissões necessárias para que eu possa te ajudar",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Para que eu possa interagir com seu dispositivo, executar comandos e usar recursos do sistema, algumas permissões padrão do Android são necessárias.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary, contentColor = DarkBackground),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Conceder Permissões", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PermissionsPanelContent(
    padding: PaddingValues,
    recordAudioGranted: Boolean,
    locationGranted: Boolean,
    smsGranted: Boolean,
    phoneGranted: Boolean,
    contactsGranted: Boolean,
    calendarGranted: Boolean,
    cameraGranted: Boolean,
    notificationsGranted: Boolean,
    storageGranted: Boolean,
    accessibilityGranted: Boolean,
    writeSettingsGranted: Boolean,
    onAudioGrant: () -> Unit,
    onLocationGrant: () -> Unit,
    onSmsPhoneGrant: () -> Unit,
    onContactsCalendarGrant: () -> Unit,
    onCameraGrant: () -> Unit,
    onNotificationsGrant: () -> Unit,
    onWriteSettingsGrant: () -> Unit,
    onStorageGrant: () -> Unit,
    onAccessibilityGrant: () -> Unit,
    onFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
    ) {
        Text(
            text = "Permissões Necessárias",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Configure as permissões abaixo para ativar todas as funções autônomas.",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionCard(
                    title = "Microfone",
                    desc = "Necessário para wake word e reconhecimento de voz.",
                    granted = recordAudioGranted,
                    onGrant = onAudioGrant
                )
            }
            item {
                PermissionCard(
                    title = "Localização",
                    desc = "Needed to fetch weather, directions, and maps.",
                    granted = locationGranted,
                    onGrant = onLocationGrant
                )
            }
            item {
                PermissionCard(
                    title = "SMS & Telefonia",
                    desc = "Necessário para ler e enviar mensagens e fazer chamadas.",
                    granted = smsGranted && phoneGranted,
                    onGrant = onSmsPhoneGrant
                )
            }
            item {
                PermissionCard(
                    title = "Contatos & Calendário",
                    desc = "Necessário para resolver nomes de contatos e gerenciar eventos.",
                    granted = contactsGranted && calendarGranted,
                    onGrant = onContactsCalendarGrant
                )
            }
            item {
                PermissionCard(
                    title = "Câmera",
                    desc = "Necessário para captura de imagens e recursos de visão.",
                    granted = cameraGranted,
                    onGrant = onCameraGrant
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionCard(
                        title = "Notificações",
                        desc = "Necessário para enviar notificações do sistema e status do serviço.",
                        granted = notificationsGranted,
                        onGrant = onNotificationsGrant
                    )
                }
            }
            item {
                PermissionCard(
                    title = "Armazenamento / Arquivos",
                    desc = "Necessário para listar, ler, escrever e deletar arquivos.",
                    granted = storageGranted,
                    onGrant = onStorageGrant
                )
            }
            item {
                PermissionCard(
                    title = "Configurações do Sistema",
                    desc = "Necessário para ajustar brilho, volume e outras configurações.",
                    granted = writeSettingsGranted,
                    onGrant = onWriteSettingsGrant
                )
            }
            item {
                PermissionCard(
                    title = "Serviço de Acessibilidade",
                    desc = "Permite automação completa da tela (cliques e digitação).",
                    granted = accessibilityGranted,
                    onGrant = onAccessibilityGrant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary, contentColor = DarkBackground),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Entrar no ZonIA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) BorderColor else AccentSecondary,
                contentColor = if (granted) TextSecondary else DarkBackground
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (granted) "Granted" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun checkPerm(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        checkPerm(context, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                checkPerm(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (com.opendroid.ai.accessibility.OpenDroidAccessibilityService.getInstance() != null) {
        return true
    }
    val expectedComponentName = android.content.ComponentName(context, com.opendroid.ai.accessibility.OpenDroidAccessibilityService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
            return true
        }
    }
    return false
}
