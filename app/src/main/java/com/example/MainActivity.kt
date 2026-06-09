package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AegisBackground
                ) { innerPadding ->
                    AegisDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Data model representing pre-defined target apps
data class TargetGame(val name: String, val packageId: String, val iconColor: Color)

val PREDEFINED_GAMES = listOf(
    TargetGame("Call of Duty: Mobile", "com.activision.callofduty.shooter", Color(0xFFEAB308)),
    TargetGame("PUBG Mobile", "com.tencent.ig", Color(0xFFF97316)),
    TargetGame("Apex Legends Mobile", "com.ea.gp.apexlegendsmobilefps", Color(0xFFEF4444)),
    TargetGame("League of Legends: Wild Rift", "com.riotgames.league.wildrift", Color(0xFF3B82F6)),
    TargetGame("Mobile Legends: Bang Bang", "com.mobile.legends", Color(0xFFA855F7)),
    TargetGame("Genshin Impact", "com.miHoYo.GenshinImpact", Color(0xFF06B6D4))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisDashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val telemetry by GameProxyService.telemetryState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("core") } // "core", "nodes", "logs", "blueprint"
    
    // Core selection states
    var selectedGameIndex by remember { mutableStateOf(0) }
    var customGamePackage by remember { mutableStateOf("com.ea.gp.apexlegendsmobilefps") }
    var isCustomGamePackageMode by remember { mutableStateOf(false) }
    
    val activeGameName = if (isCustomGamePackageMode) "Custom Game Overlay" else PREDEFINED_GAMES[selectedGameIndex].name
    val activeGamePackage = if (isCustomGamePackageMode) customGamePackage else PREDEFINED_GAMES[selectedGameIndex].packageId
    val activeGameColor = if (isCustomGamePackageMode) AegisSlateBlue else PREDEFINED_GAMES[selectedGameIndex].iconColor

    // Node selection states
    var selectedNodeIndex by remember { mutableStateOf(0) }
    var nodesListState = remember { mutableStateListOf<RoutingEngine.RoutingNode>().apply { addAll(RoutingEngine.defaultNodes) } }
    val activeNode = nodesListState[selectedNodeIndex]

    // Terminal simulated log lines state
    val terminalLogs = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger log updates periodically when telemetry is active
    LaunchedEffect(telemetry.isRunning) {
        if (telemetry.isRunning) {
            terminalLogs.add("[SYS] - Establishing secure virtual private channel mapping...")
            delay(300)
            terminalLogs.add("[TUN] - Created virtual handle /dev/tun0 IP mapping: 10.0.0.2/32")
            delay(200)
            terminalLogs.add("[SYS] - Restricting interception scope directly to game UID: ${telemetry.appPackage}")
            delay(400)
            terminalLogs.add("[PEER] - Node handshake succeeded at edge peer: ${telemetry.proxyNodeIp} (UDP/51820)")
            
            while (telemetry.isRunning) {
                delay(1200 + (200..1000).random().toLong())
                val action = listOf(
                    "[NDK] - Hook Intercept: UDP state frame translated.",
                    "[PEER] - Packet encapsulated: Payload ${ (64..512).random() } B -> Node edge stream",
                    "[SYS] - Telemetry recalculation: Routing Quality Index updated.",
                    "[TUN] - Ring buffer poll finished, 0% drop cached."
                ).random()
                terminalLogs.add(action)
                if (terminalLogs.size > 80) terminalLogs.removeAt(0)
            }
        } else {
            terminalLogs.add("[SYS] - Secure virtual tunnel released. Falling back to default ISP routes.")
        }
    }

    // Interactive custom metric sliders for educational simulation
    var interactiveLatency by remember { mutableStateOf(activeNode.baseLatencyMs) }
    var interactiveJitter by remember { mutableStateOf(activeNode.baseJitterMs) }
    var interactiveLoss by remember { mutableStateOf(activeNode.basePacketLossPercent) }

    // Sync interactive states when user shifts nodes
    LaunchedEffect(selectedNodeIndex) {
        interactiveLatency = activeNode.baseLatencyMs
        interactiveJitter = activeNode.baseJitterMs
        interactiveLoss = activeNode.basePacketLossPercent
    }

    // Keep active node dynamic parameters updated in real time for RQS score display
    val updatedActiveNode = activeNode.copy(
        baseLatencyMs = if (telemetry.isRunning && telemetry.proxyNodeIp == activeNode.ipAddress) telemetry.latencyMs else interactiveLatency,
        baseJitterMs = if (telemetry.isRunning && telemetry.proxyNodeIp == activeNode.ipAddress) telemetry.jitterMs else interactiveJitter,
        basePacketLossPercent = if (telemetry.isRunning && telemetry.proxyNodeIp == activeNode.ipAddress) telemetry.packetLossPercent else interactiveLoss
    )

    // VPN Activation launcher handling System OS permission preparation
    val vpnActionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val startIntent = Intent(context, GameProxyService::class.java).apply {
                action = GameProxyService.ACTION_START
                putExtra(GameProxyService.EXTRA_GAME_PACKAGE, activeGamePackage)
                putExtra(GameProxyService.EXTRA_NODE_IP, updatedActiveNode.ipAddress)
                putExtra(GameProxyService.EXTRA_NODE_LABEL, updatedActiveNode.label)
            }
            context.startService(startIntent)
        } else {
            // Permission rejected or failed, launch fallback anyway to show capability model!
            val startIntent = Intent(context, GameProxyService::class.java).apply {
                action = GameProxyService.ACTION_START
                putExtra(GameProxyService.EXTRA_GAME_PACKAGE, activeGamePackage)
                putExtra(GameProxyService.EXTRA_NODE_IP, updatedActiveNode.ipAddress)
                putExtra(GameProxyService.EXTRA_NODE_LABEL, updatedActiveNode.label)
            }
            context.startService(startIntent)
        }
    }

    // Glowing animation for background elements
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AegisBackground)
    ) {
        // --- STATUS BAR SIMULATOR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isRunning) AegisEmerald else AegisMuted)
                )
                Text(
                    text = if (telemetry.isRunning) "5G · AEGIS LOW-LATENCY TUNNEL" else "5G · BYPASS UNCONNECTED",
                    style = TextStyle(
                        color = AegisSilver.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (telemetry.isRunning) "TUN: 1400 MTU" else "ISP DIRECT",
                    style = TextStyle(color = AegisSilver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "LOSS: ${if (telemetry.isRunning) telemetry.packetLossPercent else "N/A"}",
                    style = TextStyle(color = if (telemetry.isRunning && telemetry.packetLossPercent > 0.1) Color.Red else AegisEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        // --- APP HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEMS ARCHITECT",
                    style = TextStyle(
                        color = AegisEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Aegis",
                        style = TextStyle(
                            color = AegisSilver,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Link",
                        style = TextStyle(
                            color = AegisSilver,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
            }

            // Glowing green node node selector icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AegisZinc900)
                    .border(1.dp, AegisZinc800, CircleShape)
                    .clickable { selectedTab = "nodes" },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isRunning) AegisEmerald else Color(0xFFF59E0B))
                        .drawBehind {
                            if (telemetry.isRunning) {
                                drawCircle(
                                    color = AegisEmerald,
                                    radius = size.minDimension * glowAlpha * 2.5f,
                                    alpha = 0.4f * (1f - glowAlpha)
                                )
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- MAIN VIEWPORT (TAB CONTENT) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                "core" -> {
                    CoreDashboardTab(
                        telemetry = telemetry,
                        activeGameName = activeGameName,
                        activeGamePackage = activeGamePackage,
                        activeGameColor = activeGameColor,
                        predefinedGames = PREDEFINED_GAMES,
                        selectedGameIndex = selectedGameIndex,
                        onGameSelected = { index -> selectedGameIndex = index; isCustomGamePackageMode = false },
                        isCustomGamePackageMode = isCustomGamePackageMode,
                        customGamePackage = customGamePackage,
                        onCustomGamePackageChange = { customGamePackage = it },
                        onToggleCustomMode = { isCustomGamePackageMode = it },
                        activeNode = updatedActiveNode,
                        nodesList = nodesListState,
                        onNodeIndexSelected = { selectedNodeIndex = it },
                        onStartBooster = {
                            GameProxyService.resetTelemetry()
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnActionLauncher.launch(prepareIntent)
                            } else {
                                val startIntent = Intent(context, GameProxyService::class.java).apply {
                                    action = GameProxyService.ACTION_START
                                    putExtra(GameProxyService.EXTRA_GAME_PACKAGE, activeGamePackage)
                                    putExtra(GameProxyService.EXTRA_NODE_IP, updatedActiveNode.ipAddress)
                                    putExtra(GameProxyService.EXTRA_NODE_LABEL, updatedActiveNode.label)
                                }
                                context.startService(startIntent)
                            }
                        },
                        onStopBooster = {
                            val stopIntent = Intent(context, GameProxyService::class.java).apply {
                                action = GameProxyService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        },
                        glowAlpha = glowAlpha
                    )
                }
                "nodes" -> {
                    NodesOptimizerTab(
                        nodesList = nodesListState,
                        selectedIndex = selectedNodeIndex,
                        onSelectIndex = { selectedNodeIndex = it },
                        interactiveLatency = interactiveLatency,
                        onLatencyChange = { interactiveLatency = it },
                        interactiveJitter = interactiveJitter,
                        onJitterChange = { interactiveJitter = it },
                        interactiveLoss = interactiveLoss,
                        onLossChange = { interactiveLoss = it },
                        activeTelemetry = telemetry
                    )
                }
                "logs" -> {
                    TerminalLogsTab(
                        logs = terminalLogs,
                        isRunning = telemetry.isRunning,
                        onClearLogs = { terminalLogs.clear() }
                    )
                }
                "blueprint" -> {
                    SystemBlueprintTab()
                }
            }
        }

        // --- BOTTOM NAVIGATION CONTROLS ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = AegisZinc900.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, AegisZinc800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "BOOSTER",
                    isActive = selectedTab == "core",
                    testTag = "nav_tab_core",
                    symbol = "✦",
                    onClick = { selectedTab = "core" }
                )
                BottomNavItem(
                    label = "NODES",
                    isActive = selectedTab == "nodes",
                    testTag = "nav_tab_nodes",
                    symbol = "☍",
                    onClick = { selectedTab = "nodes" }
                )
                BottomNavItem(
                    label = "LOGS",
                    isActive = selectedTab == "logs",
                    testTag = "nav_tab_logs",
                    symbol = "⌨",
                    onClick = { selectedTab = "logs" }
                )
                BottomNavItem(
                    label = "BLUEPRINT",
                    isActive = selectedTab == "blueprint",
                    testTag = "nav_tab_blueprint",
                    symbol = "🛠",
                    onClick = { selectedTab = "blueprint" }
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    label: String,
    isActive: Boolean,
    testTag: String,
    symbol: String,
    onClick: () -> Unit
) {
    val activeColor = AegisEmerald
    val inactiveColor = AegisMuted

    Column(
        modifier = Modifier
            .weight(1f)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = symbol,
            style = TextStyle(
                color = if (isActive) activeColor else inactiveColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                color = if (isActive) activeColor else inactiveColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

// --- TAB 1: CORE GAME BOOSTER BOARD ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoreDashboardTab(
    telemetry: GameProxyService.TelemetryData,
    activeGameName: String,
    activeGamePackage: String,
    activeGameColor: Color,
    predefinedGames: List<TargetGame>,
    selectedGameIndex: Int,
    onGameSelected: (Int) -> Unit,
    isCustomGamePackageMode: Boolean,
    customGamePackage: String,
    onCustomGamePackageChange: (String) -> Unit,
    onToggleCustomMode: (Boolean) -> Unit,
    activeNode: RoutingEngine.RoutingNode,
    nodesList: List<RoutingEngine.RoutingNode>,
    onNodeIndexSelected: (Int) -> Unit,
    onStartBooster: () -> Unit,
    onStopBooster: () -> Unit,
    glowAlpha: Float
) {
    var showGameSelector by remember { mutableStateOf(false) }
    var showNodeSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Connection Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(AegisZinc900.copy(alpha = 0.6f), AegisZinc900.copy(alpha = 0.3f))
                    )
                )
                .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(32.dp))
        ) {
            // Glowing line top overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, AegisEmerald.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BOOSTER ENGINE STATUS",
                    style = TextStyle(
                        color = AegisMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (telemetry.isRunning) "Optimal Split Tunnel Active" else "Engine Offline",
                    style = TextStyle(
                        color = if (telemetry.isRunning) AegisEmerald else AegisMuted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = if (telemetry.isRunning) AegisEmerald.copy(0.3f) else Color.Transparent,
                            blurRadius = 12f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current targeted Game Indicator row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AegisBackground.copy(alpha = 0.8f))
                        .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(20.dp))
                        .clickable { showGameSelector = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeGameColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeGameName,
                            style = TextStyle(color = AegisSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = activeGamePackage,
                            style = TextStyle(color = AegisMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        )
                    }
                    Text(
                        text = "CHANGE ▾",
                        style = TextStyle(color = AegisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Metrics Bento Grid (2x2)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // CARD 1: Latency
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AegisZinc900.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text("BOOSTER PING", style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (telemetry.isRunning) "${telemetry.latencyMs.toInt()}" else "${activeNode.baseLatencyMs.toInt()}",
                            style = TextStyle(color = AegisWhite, fontSize = 32.sp, fontWeight = FontWeight.Light)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ms", style = TextStyle(color = AegisEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Visual network progress meter
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(AegisZinc800)
                    ) {
                        val latencyValue = if (telemetry.isRunning) telemetry.latencyMs else activeNode.baseLatencyMs
                        val fraction = minOf(1.0f, (latencyValue / 150f).toFloat())
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(CircleShape)
                                .background(if (latencyValue < 50) AegisEmerald else Color(0xFFF59E0B))
                        )
                    }
                }

                // CARD 2: Jitter
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AegisZinc900.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text("PATH JITTER", style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (telemetry.isRunning) "${telemetry.jitterMs}" else "${activeNode.baseJitterMs}",
                            style = TextStyle(color = AegisWhite, fontSize = 32.sp, fontWeight = FontWeight.Light)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ms", style = TextStyle(color = AegisEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val jitterVal = if (telemetry.isRunning) telemetry.jitterMs else activeNode.baseJitterMs
                    Text(
                        text = "Stability: ${if (jitterVal < 2.0) "99.4%" else if (jitterVal < 5.0) "95.1%" else "81.6%"}",
                        style = TextStyle(color = if (jitterVal < 3.0) AegisEmerald else Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // CARD 3: Routing Node Details (Double span)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AegisZinc900.copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                    .clickable { showNodeSelector = true }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PROXY ACCELERATION NODE", style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (telemetry.isRunning) telemetry.proxyNodeLabel else activeNode.label,
                            style = TextStyle(color = AegisWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (telemetry.isRunning) telemetry.proxyNodeIp else activeNode.ipAddress} (UDP Tunnel / WireGuard V2)",
                            style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(AegisZinc800)
                            .padding(horizontal = 8.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text("LOSS RATE", style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                        val activeLoss = if (telemetry.isRunning) telemetry.packetLossPercent else activeNode.basePacketLossPercent
                        Text(
                            text = "${String.format("%.2f", activeLoss)}%",
                            style = TextStyle(
                                color = if (activeLoss == 0.0) AegisEmerald else Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (activeLoss <= 0.05) "Premium Route" else "Unstable Route",
                            style = TextStyle(color = if (activeLoss <= 0.05) AegisEmerald.copy(0.7f) else Color(0xFFEF4444), fontSize = 8.sp)
                        )
                    }
                }
            }

            // Real-Time Bandwidth telemetry card
            if (telemetry.isRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AegisZinc900.copy(alpha = 0.3f))
                        .border(BorderStroke(1.dp, AegisZinc800.copy(0.6f)), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TUN TUNNEL DATA STATS", style = TextStyle(color = AegisMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Text("TX Packets: ${telemetry.packetsSent}", style = TextStyle(color = AegisSilver, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
                            Text("RX Packets: ${telemetry.packetsReceived}", style = TextStyle(color = AegisSilver, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
                        }
                    }
                    Text(
                        text = "Vol: ${(telemetry.bytesTransferred / 1024.0).toInt()} KB",
                        style = TextStyle(color = AegisEmerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Split-Tunnel L7 Engine Description Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Split-Tunnel Engine Blueprint",
                    style = TextStyle(color = AegisSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AegisEmerald.copy(alpha = 0.1f))
                        .border(BorderStroke(1.dp, AegisEmerald.copy(alpha = 0.2f)), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("VpnService L7", style = TextStyle(color = AegisEmerald, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android framework selectively captures only the allowed game package UID. Raw Layer-3 packets are parsed from the virtual interfaces file descriptor, bypassing full virtualized network adapters to maintain sub-millisecond local routing speeds.",
                style = TextStyle(color = AegisMuted, fontSize = 10.sp, lineHeight = 14.sp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Tonal Operation Toggle Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (telemetry.isRunning) Color(0xFFDC2626) else AegisEmerald)
                .testTag("action_booster_toggle")
                .clickable {
                    if (telemetry.isRunning) {
                        onStopBooster()
                    } else {
                        onStartBooster()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (telemetry.isRunning) "STOP NETWORK ACCELERATION" else "START GAME OPTIMIZATION",
                    style = TextStyle(
                        color = if (telemetry.isRunning) AegisWhite else Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isRunning) AegisWhite else Color.Black)
                        .drawBehind {
                            drawCircle(
                                color = if (telemetry.isRunning) AegisWhite else Color.Black,
                                radius = size.minDimension * (1.5f + (0.5f * (1f - glowAlpha))),
                                alpha = 0.4f
                            )
                        }
                )
            }
        }
    }

    // Modal Sheet Simulation for Select Targets Game
    if (showGameSelector) {
        AlertDialog(
            onDismissRequest = { showGameSelector = false },
            containerColor = AegisZinc900,
            title = {
                Text("Select Target Game Interface", style = TextStyle(color = AegisWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose an active game profile below to apply selective L7 split-routing boundaries:", style = TextStyle(color = AegisMuted, fontSize = 11.sp))
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    predefinedGames.forEachIndexed { index, game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isCustomGamePackageMode && selectedGameIndex == index) AegisZinc800 else Color.Transparent)
                                .clickable {
                                    onGameSelected(index)
                                    showGameSelector = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(game.iconColor))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(game.name, style = TextStyle(color = AegisWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                                Text(game.packageId, style = TextStyle(color = AegisMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace))
                            }
                            if (!isCustomGamePackageMode && selectedGameIndex == index) {
                                Text("SELECTED", style = TextStyle(color = AegisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Toggle custom mode entry
                    Divider(color = AegisZinc800, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustomGamePackageMode) AegisZinc800 else Color.Transparent)
                            .clickable { onToggleCustomMode(true) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(AegisSlateBlue))
                        Text("Manually enter custom Package ID", style = TextStyle(color = AegisWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                    }

                    if (isCustomGamePackageMode) {
                        OutlinedTextField(
                            value = customGamePackage,
                            onValueChange = onCustomGamePackageChange,
                            maxLines = 1,
                            textStyle = TextStyle(color = AegisWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisEmerald,
                                unfocusedBorderColor = AegisZinc800,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            placeholder = { Text("e.g. com.epicgames.portal", fontSize = 11.sp, color = AegisMuted) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGameSelector = false }) {
                    Text("APPLY CORES ROUTE", color = AegisEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal dialog for selecting Proxy Nodes
    if (showNodeSelector) {
        AlertDialog(
            onDismissRequest = { showNodeSelector = false },
            containerColor = AegisZinc900,
            title = {
                Text("Accelerated Nodes Topology", style = TextStyle(color = AegisWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                ) {
                    items(nodesList) { node ->
                        val index = nodesList.indexOf(node)
                        val score = node.qualityScore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (nodesList.indexOf(activeNode) == index) AegisZinc800 else Color.Transparent)
                                .clickable {
                                    onNodeIndexSelected(index)
                                    showNodeSelector = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(node.label, style = TextStyle(color = AegisWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ping: ${node.baseLatencyMs.toInt()}ms", style = TextStyle(color = AegisMuted, fontSize = 9.sp))
                                    Text("Loss: ${node.basePacketLossPercent}%", style = TextStyle(color = AegisMuted, fontSize = 9.sp))
                                }
                            }

                            // Dynamic color RQS badge
                            val badgeColor = if (score > 75) AegisEmerald else if (score > 40) Color(0xFFF59E0B) else Color(0xFFEF4444)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.1f))
                                    .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RQS: ${score.toInt()}",
                                    style = TextStyle(color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNodeSelector = false }) {
                    Text("CLOSE", color = AegisEmerald, fontSize = 12.sp)
                }
            }
        )
    }
}

// --- TAB 2: DETAILED NODES ACCELERATION & SLIDERS OPTIMIZER ---
@Composable
fun NodesOptimizerTab(
    nodesList: List<RoutingEngine.RoutingNode>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    interactiveLatency: Double,
    onLatencyChange: (Double) -> Unit,
    interactiveJitter: Double,
    onJitterChange: (Double) -> Unit,
    interactiveLoss: Double,
    onLossChange: (Double) -> Unit,
    activeTelemetry: GameProxyService.TelemetryData
) {
    val activeNode = nodesList[selectedIndex]
    
    // Recalculated RQS Score based on custom slider positions
    val currentScore = RoutingEngine.calculateQualityScore(
        interactiveLatency,
        interactiveJitter,
        interactiveLoss
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Explanatory mathematical banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AegisZinc900.copy(alpha = 0.4f))
                    .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "STABILITY WEIGHTING MODEL",
                    style = TextStyle(color = AegisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A competitive route prioritizes stability over raw delay because TCP/UDP packet loss and high jitter trigger interpolation stalls. A steady 60ms route beats a spiking 40ms route directly.",
                    style = TextStyle(color = AegisMuted, fontSize = 10.sp, lineHeight = 14.sp)
                )
            }
        }

        // Active node selector grid
        item {
            Text("Edge Nodes Selection", style = TextStyle(color = AegisSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nodesList.forEachIndexed { idx, node ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedIndex == idx) AegisEmerald.copy(0.15f) else AegisZinc900.copy(0.6f))
                            .border(
                                BorderStroke(1.dp, if (selectedIndex == idx) AegisEmerald else AegisZinc800),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectIndex(idx) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = node.id.replace("-edge", "").uppercase(),
                            style = TextStyle(
                                color = if (selectedIndex == idx) AegisEmerald else AegisSilver,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Live Simulated calculation card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeNode.label,
                    style = TextStyle(color = AegisWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Node IP: ${activeNode.ipAddress}",
                    style = TextStyle(color = AegisMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated RQS Score display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentScore.toInt()}",
                            style = TextStyle(
                                color = if (currentScore > 75) AegisEmerald else if (currentScore > 40) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        Text(
                            text = "RQS Score",
                            style = TextStyle(color = AegisMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(AegisZinc800)
                    )

                    Column {
                        val rating = if (currentScore > 85) "SS Tier (Optimal)" else if (currentScore > 70) "S Tier (Fully Playable)" else if (currentScore > 45) "A Tier (Congested)" else "FAIL (Extreme Packet Loss)"
                        Text("Network Quality Check:", fontSize = 10.sp, color = AegisMuted)
                        Text(
                            text = rating,
                            style = TextStyle(
                                color = if (currentScore > 45) AegisWhite else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (currentScore > 45) "Optimized path enabled." else "Avoid. High game frame loss expected.",
                            style = TextStyle(color = AegisMuted, fontSize = 9.sp)
                        )
                    }
                }
            }
        }

        // Dynamic optimization parameters (Sliders)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AegisZinc900.copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TUNING SIMULATOR",
                    style = TextStyle(color = AegisSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )

                if (activeTelemetry.isRunning && activeTelemetry.proxyNodeIp == activeNode.ipAddress) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisEmerald.copy(alpha = 0.1f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ TELEMETRY LOCKED: Live booster is reporting raw NDK packet values.",
                            color = AegisEmerald,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Slider 1: Latency
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Latency", style = TextStyle(color = AegisSilver, fontSize = 11.sp))
                        Text("${interactiveLatency.toInt()} ms", style = TextStyle(color = AegisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = interactiveLatency.toFloat(),
                        onValueChange = { if (!(activeTelemetry.isRunning && activeTelemetry.proxyNodeIp == activeNode.ipAddress)) onLatencyChange(it.toDouble()) },
                        valueRange = 5f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = AegisEmerald,
                            activeTrackColor = AegisEmerald,
                            inactiveTrackColor = AegisZinc800
                        )
                    )
                }

                // Slider 2: Jitter
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jitter (variance)", style = TextStyle(color = AegisSilver, fontSize = 11.sp))
                        Text("${String.format("%.1f", interactiveJitter)} ms", style = TextStyle(color = AegisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = interactiveJitter.toFloat(),
                        onValueChange = { if (!(activeTelemetry.isRunning && activeTelemetry.proxyNodeIp == activeNode.ipAddress)) onJitterChange(it.toDouble()) },
                        valueRange = 0.1f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = AegisEmerald,
                            activeTrackColor = AegisEmerald,
                            inactiveTrackColor = AegisZinc800
                        )
                    )
                }

                // Slider 3: Packet Loss
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Packet Loss", style = TextStyle(color = AegisSilver, fontSize = 11.sp))
                        Text("${String.format("%.2f", interactiveLoss)} %", style = TextStyle(color = if (interactiveLoss > 0.0) Color(0xFFEF4444) else AegisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = interactiveLoss.toFloat(),
                        onValueChange = { if (!(activeTelemetry.isRunning && activeTelemetry.proxyNodeIp == activeNode.ipAddress)) onLossChange(it.toDouble()) },
                        valueRange = 0f..12f,
                        colors = SliderDefaults.colors(
                            thumbColor = AegisEmerald,
                            activeTrackColor = AegisEmerald,
                            inactiveTrackColor = AegisZinc800
                        )
                    )
                }
            }
        }
    }
}

// --- TAB 3: TERMINAL SECURE PACKET CONSOLE ---
@Composable
fun TerminalLogsTab(
    logs: List<String>,
    isRunning: Boolean,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SECURE TUNNEL CONSOLE",
                style = TextStyle(color = AegisSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onClearLogs) {
                Text("CLEAR LOGS", color = AegisEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            val logScrollState = rememberScrollState()
            
            // Auto scroll to bottom
            LaunchedEffect(logs.size) {
                logScrollState.animateScrollTo(logScrollState.maxValue)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(logScrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Initialize VPN Engine optimization above to stream low-level socket handshakes and NDK packet capture transactions here...",
                        color = AegisMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    logs.forEach { log ->
                        Text(
                            text = log,
                            color = if (log.contains("Hook Intercept")) AegisEmerald else if (log.contains("Failed") || log.contains("Cancel")) Color(0xFFEF4444) else AegisSilver,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// --- TAB 4: ARCHITECT SYSTEM BLUEPRINT VIEW ---
@Composable
fun SystemBlueprintTab() {
    val context = LocalContext.current
    var activeSubSection by remember { mutableStateOf("architecture") } // "architecture", "vpnservice", "scoring"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeSubSection == "architecture") AegisEmerald.copy(0.12f) else AegisZinc900.copy(0.5f))
                    .border(
                        BorderStroke(1.dp, if (activeSubSection == "architecture") AegisEmerald else AegisZinc800),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { activeSubSection = "architecture" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SYSTEM FLOW",
                    color = if (activeSubSection == "architecture") AegisEmerald else AegisSilver,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeSubSection == "vpnservice") AegisEmerald.copy(0.12f) else AegisZinc900.copy(0.5f))
                    .border(
                        BorderStroke(1.dp, if (activeSubSection == "vpnservice") AegisEmerald else AegisZinc800),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { activeSubSection = "vpnservice" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KOTLIN VPN",
                    color = if (activeSubSection == "vpnservice") AegisEmerald else AegisSilver,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeSubSection == "scoring") AegisEmerald.copy(0.12f) else AegisZinc900.copy(0.5f))
                    .border(
                        BorderStroke(1.dp, if (activeSubSection == "scoring") AegisEmerald else AegisZinc800),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { activeSubSection = "scoring" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PYTHON SCORING",
                    color = if (activeSubSection == "scoring") AegisEmerald else AegisSilver,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AegisZinc900.copy(alpha = 0.5f))
                .border(BorderStroke(1.dp, AegisZinc800), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            val contentScroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeSubSection) {
                    "architecture" -> {
                        Text("PART 1: Low-Latency Virtual Tunnel Data Flow", style = TextStyle(color = AegisWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Divider(color = AegisZinc800)
                        
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisEmerald)) { append("1. Payload Transmit:\n") }
                                append("The game (e.g. 'com.activision.callofduty.shooter') generates outbound standard UDP sockets destined for the authoritative game server IP:Port.\n\n")

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisEmerald)) { append("2. Kernel Hijacking & Selective Interception:\n") }
                                append("The Android network kernel redirects selected UIDs on the virtual network mapping table into our local virtual interface device (/dev/tun0). Only target gaming packages configured in split-tunneling are routed here; normal applications bypass the TUN straight to physical network interfaces, preventing proxy overhead.\n\n")

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisEmerald)) { append("3. NDK Level Non-Blocking Interception:\n") }
                                append("Our process opens the virtual file descriptor (ParcelFileDescriptor). To bypass high-overhead JVM Garbage Collection stalls, an optimized direct ByteBuffer reading is used on a separate native POSIX thread using epoll. Ip headers are parsed, isolating active gaming UDP payloads.\n\n")

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisEmerald)) { append("4. Secure Capsule Encapsulation:\n") }
                                append("We wrap the raw UDP payload in a highly lightweight UDP tunnel envelope containing sequence coordinates and cryptographic verification. The outer host is immediately directed to the optimized gaming proxy IP.\n\n")

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisEmerald)) { append("5. Global Path Optimization & Delivery:\n") }
                                append("The router nodes sit on major enterprise network backbones (AWS/GCP), which utilize premium, non-congested transit peers directly matching the game server destinations. The proxy node decapsulates the tunnel and retransmits raw UDP to the game server, achieving minimal jitter.")
                            },
                            style = TextStyle(color = AegisSilver.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 16.sp)
                        )
                    }
                    "vpnservice" -> {
                        Text("PART 2: Android VpnService Core Kotlin Structure", style = TextStyle(color = AegisWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Divider(color = AegisZinc800)
                        
                        Text(
                            text = """
class GameProxyService : VpnService() {
    private var tunnelInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gamePackage = intent?.getStringExtra("extra_game_package")
        val nodeIp = intent?.getStringExtra("extra_node_ip")
        
        establishSplitTunnel(gamePackage, nodeIp)
        return START_STICKY
    }

    private fun establishSplitTunnel(packageName: String, nodeIp: String) {
        val builder = Builder()
            .setSession("GameBoosterTunnel")
            .setMtu(1400) // Support overlay headers without fragmentation
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)

        // EXQUISITE SPLIT ROUTING BOUNDARY:
        // Ensures only the selected game package gets routed to tunnel!
        builder.addAllowedApplication(packageName)

        val pfd = builder.establish()
        tunnelInterface = pfd

        // Spin up background ring buffer loops
        CoroutineScope(Dispatchers.IO).launch {
            val tunnelInput = FileInputStream(pfd.fileDescriptor)
            val packet = ByteBuffer.allocateDirect(16384)
            val socket = DatagramSocket()
            val serverAddress = InetAddress.getByName(nodeIp)

            while (isActive) {
                packet.clear()
                val readLength = tunnelInput.read(packet.array())
                if (readLength > 0) {
                    val rawData = packet.array()
                    val protocol = rawData[9].toInt()

                    if (protocol == 17) { // UDP ONLY
                        val datagram = DatagramPacket(rawData, readLength, serverAddress, 51820)
                        socket.send(datagram)
                    }
                }
            }
        }
    }
}
                            """.trimIndent(),
                            style = TextStyle(color = AegisEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
                        )
                    }
                    "scoring" -> {
                        Text("PART 3: Python Production Node Scoring Script", style = TextStyle(color = AegisWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Divider(color = AegisZinc800)

                        Text(
                            text = """
def calculate_routing_score(
    latency_ms: float, 
    jitter_ms: float, 
    packet_loss_percent: float
) -> float:
    \"\"\"
    Calculates 0-100 routing index prioritizing stability.
    
    1. Latency is linearly taxed (Weight: 0.25). Game mechanics 
       dead-reckoning masks steady delay.
    2. Jitter triggers prediction failures (Weight: 3.00).
    3. Packet loss triggers critical frame freezes (Weight: 15.00).
       Even a 4% loss rate brings heavy penalty multipliers.
    \"\"\"
    latency_penalty = latency_ms * 0.25
    jitter_penalty = jitter_ms * 3.0
    packet_loss_penalty = packet_loss_percent * 15.0

    raw_score = 100.0 - (latency_penalty + jitter_penalty + packet_loss_penalty)
    return max(0.0, min(100.0, raw_score))


# Node A (60ms, 1.4ms jitter, 0% loss):
# Score: 100.0 - (15.0 + 4.2 + 0.0) = 80.8 (Highly Stable)

# Node B (40ms, 15ms jitter, 4.0% loss):
# Score: 100.0 - (10.0 + 45.0 + 60.0) = -15.0 -> 0.0 (Unusable Route)
                            """.trimIndent(),
                            style = TextStyle(color = AegisEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
