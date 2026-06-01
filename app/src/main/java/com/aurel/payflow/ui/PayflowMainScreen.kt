package com.aurel.payflow.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.aurel.payflow.PayflowApp
import com.aurel.payflow.data.HistoryEntry
import com.aurel.payflow.data.QueueStatus
import com.aurel.payflow.data.RoutingRule
import com.aurel.payflow.service.SmsForegroundService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayflowMainScreen(
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = PayflowApp.database
    val ruleDao = db.ruleDao()
    val historyDao = db.historyDao()

    val rules by ruleDao.getAllRulesFlow().collectAsState(initial = emptyList())
    val history by historyDao.getAllFlow().collectAsState(initial = emptyList())

    val prefs = remember { context.getSharedPreferences("PayflowPrefs", Context.MODE_PRIVATE) }
    var globalWebhook by remember { mutableStateOf(prefs.getString("webhook_url", "") ?: "") }
    var globalKey by remember { mutableStateOf(prefs.getString("global_key", "") ?: "") }
    var startDate by remember { mutableStateOf(prefs.getString("start_date", "") ?: "") }
    var monitorActive by remember { mutableStateOf(prefs.getBoolean("monitor_active", true)) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }

    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val allPermissionsGranted = hasSmsPermission && hasNotificationPermission

    LaunchedEffect(monitorActive, allPermissionsGranted) {
        val intent = Intent(context, SmsForegroundService::class.java)
        if (monitorActive && allPermissionsGranted) {
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PayFlow",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val isActive = monitorActive && allPermissionsGranted
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (isActive) "Actif" else "Inactif",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Guide", tint = Color.White)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Parametres", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A56DB)
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddRuleDialog = true },
                    containerColor = Color(0xFF1A56DB),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter une regle")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Règles de tri") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1A56DB),
                        selectedTextColor = Color(0xFF1A56DB),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("Historique") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1A56DB),
                        selectedTextColor = Color(0xFF1A56DB),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF3F4F6))
        ) {
            if (!allPermissionsGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Permissions requises",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                "PayFlow a besoin des autorisations SMS et Notifications pour fonctionner en arriere-plan.",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Text("Activer", color = Color.White)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                RulesTabContent(
                    rules = rules,
                    onToggleRule = { rule ->
                        scope.launch {
                            ruleDao.update(rule.copy(enabled = !rule.enabled))
                        }
                    },
                    onDeleteRule = { rule ->
                        scope.launch {
                            ruleDao.delete(rule)
                        }
                    }
                )
            } else {
                HistoryTabContent(
                    history = history,
                    onClearAll = {
                        scope.launch {
                            historyDao.clearAll()
                        }
                    },
                    onDeleteEntry = { entry ->
                        scope.launch {
                            historyDao.deleteById(entry.id)
                        }
                    }
                )
            }
        }
    }

    if (showSettingsDialog) {
        GlobalSettingsDialog(
            currentWebhook = globalWebhook,
            currentKey = globalKey,
            currentStartDate = startDate,
            currentMonitorActive = monitorActive,
            onDismiss = { showSettingsDialog = false },
            onSave = { webhook, key, date, active ->
                prefs.edit().apply {
                    putString("webhook_url", webhook)
                    putString("global_key", key)
                    putString("start_date", date)
                    putBoolean("monitor_active", active)
                    apply()
                }
                globalWebhook = webhook
                globalKey = key
                startDate = date
                monitorActive = active
                showSettingsDialog = false
            }
        )
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onSave = { name, senderFilter, keywords, amountKeyword, webhookUrl, accessKey ->
                scope.launch {
                    val newRule = RoutingRule(
                        name = name,
                        senderFilter = senderFilter,
                        keywords = keywords,
                        amountKeyword = amountKeyword,
                        webhookUrl = webhookUrl,
                        accessKey = accessKey,
                        enabled = true
                    )
                    ruleDao.insert(newRule)
                }
                showAddRuleDialog = false
            }
        )
    }

    if (showGuideDialog) {
        GuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
fun RulesTabContent(
    rules: List<RoutingRule>,
    onToggleRule: (RoutingRule) -> Unit,
    onDeleteRule: (RoutingRule) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aucune règle configurée", color = Color.Gray)
                Text("Appuyez sur + pour créer une règle", color = Color.Gray, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { onToggleRule(rule) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onDeleteRule(rule) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (rule.senderFilter.isNotEmpty()) {
                            Text("Filtre expéditeur : ${rule.senderFilter}", fontSize = 13.sp, color = Color.DarkGray)
                        }
                        Text("Mots-clés : ${rule.keywords.replace(".", ", ")}", fontSize = 13.sp, color = Color.DarkGray)
                        if (rule.amountKeyword.isNotEmpty()) {
                            Text("Mot-clé montant : ${rule.amountKeyword}", fontSize = 13.sp, color = Color.DarkGray)
                        }
                        if (rule.webhookUrl.isNotEmpty()) {
                            Text("Webhook spécifique configuré", fontSize = 12.sp, color = Color(0xFF1A56DB))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    history: List<HistoryEntry>,
    onClearAll: () -> Unit,
    onDeleteEntry: (HistoryEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Historique des SMS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Tout effacer", color = Color(0xFFEF4444))
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Historique vide", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { entry ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    val dateStr = sdf.format(Date(entry.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val statusColor = when (entry.status) {
                                        QueueStatus.SUCCESS -> Color(0xFF10B981)
                                        QueueStatus.PENDING -> Color(0xFFF59E0B)
                                        QueueStatus.FAILED -> Color(0xFFEF4444)
                                    }
                                    val statusText = when (entry.status) {
                                        QueueStatus.SUCCESS -> "Transmis"
                                        QueueStatus.PENDING -> "En attente"
                                        QueueStatus.FAILED -> "Échoué"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { onDeleteEntry(entry) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(entry.smsText, fontSize = 14.sp)
                            
                            if (entry.amount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Montant extrait : ${entry.amount} FCFA",
                                    color = Color(0xFF1A56DB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            if (entry.ruleName.isNotEmpty()) {
                                Text("Règle appliquée : ${entry.ruleName}", fontSize = 12.sp, color = Color.Gray)
                            }
                            if (entry.errorMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Erreur : ${entry.errorMessage}",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSettingsDialog(
    currentWebhook: String,
    currentKey: String,
    currentStartDate: String,
    currentMonitorActive: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var webhook by remember { mutableStateOf(currentWebhook) }
    var key by remember { mutableStateOf(currentKey) }
    var date by remember { mutableStateOf(currentStartDate) }
    var active by remember { mutableStateOf(currentMonitorActive) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Paramètres Globaux",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A56DB)
                )

                OutlinedTextField(
                    value = webhook,
                    onValueChange = { webhook = it },
                    label = { Text("Webhook URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Global Key") },
                    placeholder = { Text("Votre clé secrète") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date de départ (yyyy-MM-dd HH:mm)") },
                    placeholder = { Text("Ex: 2026-06-01 00:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Surveillance active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { onSave(webhook, key, date, active) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB))
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var senderFilter by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var amountKeyword by remember { mutableStateOf("") }
    var webhookUrl by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Ajouter une Règle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A56DB)
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom de la règle") },
                        placeholder = { Text("Ex: MTN Dépôt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = senderFilter,
                        onValueChange = { senderFilter = it },
                        label = { Text("Filtre Expéditeur (Optionnel)") },
                        placeholder = { Text("Ex: MTNMoney") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("Mots-clés (Ex: Depot.recu)") },
                        placeholder = { Text("Ex: Depot.recu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = amountKeyword,
                        onValueChange = { amountKeyword = it },
                        label = { Text("Mot-clé montant (Optionnel)") },
                        placeholder = { Text("Ex: recu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook spécifique (Optionnel)") },
                        placeholder = { Text("Laisser vide pour utiliser global") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = accessKey,
                        onValueChange = { accessKey = it },
                        label = { Text("Clé d'accès spécifique (Optionnel)") },
                        placeholder = { Text("Laisser vide pour utiliser global") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (name.isNotEmpty() && keywords.isNotEmpty()) {
                                    onSave(name, senderFilter, keywords, amountKeyword, webhookUrl, accessKey)
                                }
                            },
                            enabled = name.isNotEmpty() && keywords.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB))
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Guide de configuration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A56DB)
                )

                Text(
                    "Pour garantir le fonctionnement stable de PayFlow :\n\n" +
                            "1. Autorisez l'accès aux SMS.\n" +
                            "2. Désactivez l'optimisation de batterie pour PayFlow dans les paramètres du téléphone.\n" +
                            "3. Autorisez le Démarrage automatique (Autostart) pour l'application.\n" +
                            "4. Verrouillez l'application avec un cadenas dans le gestionnaire de tâches (récents).\n" +
                            "5. Configurez la Global Key et l'URL du webhook.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB))
                ) {
                    Text("Compris !")
                }
            }
        }
    }
}
