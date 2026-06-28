package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import com.example.ui.theme.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Conversation
import com.example.data.MemoryItem
import com.example.data.Message
import com.example.data.UploadedFile
import com.example.viewmodel.JarvisViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val memoryItems by viewModel.memoryItems.collectAsStateWithLifecycle()
    val uploadedFiles by viewModel.uploadedFiles.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Smart back press handling inspired by user request
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val isDrawerOpen = drawerState.isOpen
    BackHandler(enabled = true) {
        if (isDrawerOpen) {
            scope.launch { drawerState.close() }
        } else if (viewModel.currentTab != "accueil") {
            viewModel.currentTab = "accueil"
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Appuyez à nouveau pour quitter", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .glassmorphic(
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 8.dp)
                    ) {
                        // Glowing / pulsing orb indicator
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "JARVIS OS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Système actif",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation Items
                    val navItems = listOf(
                        Triple("accueil", "Accueil", Icons.Rounded.Home),
                        Triple("conversations", "Discussions", Icons.Rounded.ChatBubble),
                        Triple("generation", "Générateur IA", Icons.Rounded.AutoAwesome),
                        Triple("fichiers", "Documents", Icons.Rounded.Folder),
                        Triple("parametres", "Configuration", Icons.Rounded.Settings)
                    )

                    navItems.forEach { (tabId, label, icon) ->
                        val isSelected = viewModel.currentTab == tabId
                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = label, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.currentTab = tabId
                                viewModel.stopSpeaking()
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("nav_$tabId")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Recent Chats subsection in drawer
                    Text(
                        "Dernières discussions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    val recentChats = conversations.take(4)
                    if (recentChats.isEmpty()) {
                        Text(
                            "Aucun historique",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        recentChats.forEach { chat ->
                            val isSelectedChat = viewModel.activeConversationId.value == chat.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelectedChat) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectConversation(chat.id)
                                        viewModel.currentTab = "accueil"
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Message,
                                    contentDescription = null,
                                    tint = if (isSelectedChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = chat.title,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    color = if (isSelectedChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelectedChat) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // System Stats Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bdd locale", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("SQLITE CONNECTÉ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode Voix", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("SYNCHRONISÉ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Animated subtle background lines/glow
                BackgroundGridGlow()

                AnimatedContent(
                    targetState = viewModel.currentTab,
                    transitionSpec = {
                        val enterTransition = fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)) +
                                slideInHorizontally(
                                    initialOffsetX = { width -> (width * 0.12f).toInt() },
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
                                ) +
                                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f))
                        
                        val exitTransition = fadeOut(animationSpec = tween(200)) +
                                slideOutHorizontally(
                                    targetOffsetX = { width -> -(width * 0.08f).toInt() },
                                    animationSpec = tween(200)
                                ) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(200))

                        enterTransition togetherWith exitTransition
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "accueil" -> TabAccueil(viewModel, activeMessages, onMenuClick = { scope.launch { drawerState.open() } })
                        "conversations" -> TabConversations(viewModel, conversations, onMenuClick = { scope.launch { drawerState.open() } })
                        "generation" -> TabGeneration(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        "fichiers" -> TabFichiers(viewModel, uploadedFiles, onMenuClick = { scope.launch { drawerState.open() } })
                        "parametres" -> TabParametres(viewModel, memoryItems, onMenuClick = { scope.launch { drawerState.open() } })
                    }
                }
            }
        }
    }
}

// --- SUBTLE BACKGROUND GLOW ---
@Composable
fun BackgroundGridGlow() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Radial ambient glow at the center-bottom
                drawRect(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent)
                        } else {
                            listOf(primaryColor.copy(alpha = 0.05f), Color.Transparent)
                        },
                        center = Offset(size.width / 2, size.height * 0.7f),
                        radius = size.width * 0.8f
                    )
                )

                // Subtle technical grid lines
                val numGridLines = 15
                val cellWidth = size.width / numGridLines
                val cellHeight = size.height / numGridLines
                val gridColor = if (isDark) primaryColor.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.015f)

                for (i in 1..numGridLines) {
                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(i * cellWidth, 0f),
                        end = Offset(i * cellWidth, size.height),
                        strokeWidth = 1f
                    )
                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, i * cellHeight),
                        end = Offset(size.width, i * cellHeight),
                        strokeWidth = 1f
                    )
                }
            }
    )
}

@Composable
fun Modifier.glassmorphic(
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this
    .shadow(
        elevation = 8.dp,
        shape = shape,
        clip = false,
        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    )
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = 0.6f),
                backgroundColor.copy(alpha = 0.3f)
            )
        ),
        shape = shape
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.4f),
                borderColor.copy(alpha = 0.1f),
                borderColor.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = shape
    )

@Composable
fun Modifier.framerMotionEntry(
    delayMillis: Int = 0,
    durationMillis: Int = 400,
    stiffness: Float = 350f,
    dampingRatio: Float = 0.65f
): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "framerAlpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "framerScale"
    )
    
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 45f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "framerTranslate"
    )
    
    return this
        .graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
            this.translationY = translateY
        }
}

// ==========================================
// SCREEN 1: ACCUEIL (GLOWING SPHERE & CHAT)
// ==========================================
@Composable
fun TabAccueil(viewModel: JarvisViewModel, messages: List<Message>, onMenuClick: () -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRealSpeechRecognition()
        } else {
            viewModel.isDictating = true
            viewModel.voiceModeState = "listening"
        }
    }

    // Keep list scrolled to bottom on new messages
    LaunchedEffect(messages.size, viewModel.activeTypewriterText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu principal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "JARVIS",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Système d'intelligence active",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick New Chat Button
            IconButton(
                onClick = {
                    viewModel.startNewConversation()
                    Toast.makeText(viewModel.getApplication(), "Nouvelle session initialisée", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nouveau chat", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // --- GLOWING SPHERE / CONTEXT DISPLAY AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (messages.isEmpty()) {
                // Welcome screen with massive glowing sphere
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlowingSphere(
                        state = viewModel.voiceModeState,
                        modifier = Modifier
                            .size(220.dp)
                            .framerMotionEntry(delayMillis = 50)
                            .testTag("glowing_sphere")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "En attente d'instructions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.framerMotionEntry(delayMillis = 150)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bonjour ${viewModel.userName}, comment puis-je vous servir aujourd'hui ?",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .framerMotionEntry(delayMillis = 250)
                            .padding(horizontal = 32.dp)
                    )
                }
            } else {
                // Render beautiful dialogue list with smaller floating sphere in top-right corner
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                activeTypewriterText = if (message.id == messages.lastOrNull()?.id && viewModel.isGenerating) {
                                    viewModel.activeTypewriterText
                                } else "",
                                onCopy = {
                                    copyToClipboard(viewModel.getApplication(), message.content)
                                },
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onRegenerate = { viewModel.regenerateMessage(message.id) },
                                onEdit = { newText -> viewModel.editMessage(message.id, newText) },
                                onSpeak = { viewModel.speakText(message.content) },
                                isSpeaking = viewModel.isSpeakingTts && messages.lastOrNull()?.id == message.id,
                                sizeMultiplier = viewModel.textSizeMultiplier
                            )
                        }

                        if (viewModel.isGenerating && viewModel.activeTypewriterText.isEmpty()) {
                            item {
                                JarvisMessageSkeleton()
                            }
                        }
                    }

                    // Floating Glowing Sphere representing AI Status
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .padding(4.dp)
                    ) {
                        GlowingSphere(
                            state = viewModel.voiceModeState,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    // Interruption Overlay Floating Button
                    if (viewModel.isGenerating) {
                        Button(
                            onClick = { viewModel.stopGeneration() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Arrêter", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Arrêter la génération", fontSize = 12.sp)
                        }
                    } else if (viewModel.isSpeakingTts) {
                        Button(
                            onClick = { viewModel.stopSpeaking() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Rounded.VolumeOff, contentDescription = "Muet", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Arrêter la lecture", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- ATTACHED FILE INDICATOR PREVIEW ---
        viewModel.selectedAttachedFile?.let { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (file.mimeType.startsWith("image/")) Icons.Rounded.Image else Icons.Rounded.Description,
                        contentDescription = "Fichier",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${file.mimeType} • ${file.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.selectedAttachedFile = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- BOTTOM INPUT TEXTBAR & VOICE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dictation Animation Sheet Toggle or Attach Button
            IconButton(
                onClick = {
                    // Direct quick-attach file modal toggle
                    viewModel.currentTab = "fichiers"
                    Toast.makeText(viewModel.getApplication(), "Sélectionnez un fichier à joindre", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Joindre un document", tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Input Field
            TextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Parler avec Jarvis...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .glassmorphic(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("chat_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputQuery.trim().isNotEmpty() || viewModel.selectedAttachedFile != null) {
                            viewModel.sendMessage(inputQuery)
                            inputQuery = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Microphone Voice Button or Send Button
            val hasQueryText = inputQuery.trim().isNotEmpty() || viewModel.selectedAttachedFile != null

            IconButton(
                onClick = {
                    if (hasQueryText) {
                        viewModel.sendMessage(inputQuery)
                        inputQuery = ""
                    } else {
                        // Check if RECORD_AUDIO permission is granted
                        val hasRecordPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasRecordPermission) {
                            viewModel.startRealSpeechRecognition()
                        } else {
                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (hasQueryText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                    .testTag("send_voice_button")
            ) {
                Icon(
                    imageVector = if (hasQueryText) Icons.Rounded.Send else Icons.Rounded.Mic,
                    contentDescription = if (hasQueryText) "Envoyer" else "Parler",
                    tint = if (hasQueryText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // --- FULL SCREEN INTERACTIVE DICTATION MODAL ---
    if (viewModel.isDictating) {
        VoiceDictationDialog(
            viewModel = viewModel,
            onResult = { result ->
                inputQuery = result
                viewModel.isDictating = false
                viewModel.voiceModeState = "idle"
            },
            onCancel = {
                viewModel.isDictating = false
                viewModel.voiceModeState = "idle"
            }
        )
    }

    // --- FULL SCREEN REALSPEECH INTERACTIVE DICTATION ---
    if (viewModel.isSpeechListening) {
        VoiceListeningActiveDialog(viewModel = viewModel)
    }
}

// --- VOICE DICTATION SIMULATION SCREEN ---
@Composable
fun VoiceDictationDialog(
    viewModel: JarvisViewModel,
    onResult: (String) -> Unit,
    onCancel: () -> Unit
) {
    var timerSeconds by remember { mutableStateOf(0) }
    var detectedWords by remember { mutableStateOf("Jarvis est à votre écoute...") }
    val scope = rememberCoroutineScope()

    // Pre-made realistic spoken phrases for immersive experience
    val simulatedPhrases = listOf(
        "Quel est l'état de mes serveurs ?",
        "Jarvis, résume-moi le rapport financier s'il te plaît.",
        "Rédige un script Python pour analyser un fichier CSV.",
        "Quelle est la distance de la Terre à la Lune ?",
        "Jarvis, active la recherche web et trouve des infos sur les vols SpaceX.",
        "Explique-moi la théorie de la relativité simplement."
    )

    LaunchedEffect(Unit) {
        // Increment timer
        while (true) {
            delay(1000)
            timerSeconds++
            if (timerSeconds == 2) {
                detectedWords = simulatedPhrases.random()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(enabled = false) {}, // Prevent backclicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "JARVIS DICTÉE VOCALE",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "00:${timerSeconds.toString().padStart(2, '0')}",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Glowing pulsing voice sphere
            Box(contentAlignment = Alignment.Center) {
                // Outer rotating ring
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing))
                )
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .rotate(rotationAngle)
                        .drawBehind {
                            drawCircle(
                                color = CyanPrimary.copy(alpha = 0.15f),
                                radius = size.width / 2,
                                style = Stroke(2.dp.toPx())
                            )
                        }
                )

                GlowingSphere(state = "listening", modifier = Modifier.size(110.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Realtime Transcribing Text Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\" $detectedWords \"",
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        // Send translated string
                        onResult(if (detectedWords == "Jarvis est à votre écoute...") "Bonjour Jarvis" else detectedWords)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirmer l'envoi")
                }
            }
        }
    }
}

// --- VOICE LISTENING ACTIVE DIALOG FOR REAL SPEECH RECOGNITION ---
@Composable
fun VoiceListeningActiveDialog(
    viewModel: JarvisViewModel
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(enabled = false) {}, // Prevent backclicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "MOTEUR VOCAL JARVIS ACTIF",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                "ÉCOUTE ACTIVE DEPUIS LE MICROPHONE",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Glowing pulsing voice sphere
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulseScale)
                        .rotate(rotationAngle)
                        .drawBehind {
                            drawCircle(
                                color = CyanPrimary.copy(alpha = 0.2f),
                                radius = size.width / 2,
                                style = Stroke(2.dp.toPx())
                            )
                        }
                )

                GlowingSphere(state = "listening", modifier = Modifier.size(115.dp))
                
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Realtime Transcribing Text Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.recognizedLiveText.trim().isEmpty()) {
                            "\" Parlez maintenant, je vous écoute... \""
                        } else {
                            "\" ${viewModel.recognizedLiveText} \""
                        },
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        color = if (viewModel.recognizedLiveText.trim().isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Waveform representing active capture
                    LiveAudioWaveform(isActive = true)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.stopRealSpeechRecognition()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        viewModel.stopRealSpeechRecognition()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Traiter la commande")
                }
            }
        }
    }
}

// --- CONVERSATION DIALOGUE CARD / MESSAGE BUBBLE ---
@Composable
fun MessageBubble(
    message: Message,
    activeTypewriterText: String,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: (String) -> Unit,
    onSpeak: () -> Unit,
    isSpeaking: Boolean,
    sizeMultiplier: Float
) {
    val isUser = message.role == "user"
    var isEditing by remember { mutableStateOf(false) }
    var editTextInput by remember { mutableStateOf(message.content) }
    var showActionsMenu by remember { mutableStateOf(false) }

    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    // Sleek premium entrance animation for new message bubbles
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
    val slideAnim by animateDpAsState(
        targetValue = if (visible) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alphaAnim
                translationY = slideAnim.toPx()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // --- SENDER LABEL ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            if (!isUser) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isUser) "Vous" else "JARVIS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // --- MESSAGE CONTENT CONTAINER ---
        val bubbleShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 2.dp,
            bottomEnd = if (isUser) 2.dp else 16.dp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("message_bubble")
                .glassmorphic(
                    borderColor = if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    backgroundColor = if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    shape = bubbleShape
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attached File Preview inside Message if any
                if (message.attachedFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (message.attachedFileType?.startsWith("image/") == true) Icons.Rounded.Image else Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.attachedFileName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isEditing) {
                    TextField(
                        value = editTextInput,
                        onValueChange = { editTextInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(onClick = { isEditing = false }, contentPadding = PaddingValues(0.dp)) {
                            Text("Annuler", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                onEdit(editTextInput)
                                isEditing = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Enregistrer", fontSize = 11.sp)
                        }
                    }
                } else {
                    // Display text or active streaming typing text
                    val displayText = if (activeTypewriterText.isNotEmpty()) activeTypewriterText else message.content
                    
                    // Render premium custom Markdown formatting
                    MarkdownTextViewer(text = displayText, sizeMultiplier = sizeMultiplier)
                }
            }
        }

        // --- SUB MESSAGE QUICK ACTIONS MENU ---
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            }

            if (isUser) {
                IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
            } else {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Régénérer", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.VolumeUp else Icons.Outlined.VolumeUp,
                        contentDescription = "Écouter",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// --- PREMIUM CUSTOM LIGHTWEIGHT MARKDOWN VIEWER ---
@Composable
fun MarkdownTextViewer(text: String, sizeMultiplier: Float) {
    val lines = text.split("\n")
    var insideCodeBlock = false
    var codeBlockBuilder = StringBuilder()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            if (line.trim().startsWith("```")) {
                if (insideCodeBlock) {
                    // Render current Code Block Card
                    CodeBlockCard(code = codeBlockBuilder.toString().trim())
                    codeBlockBuilder = StringBuilder()
                    insideCodeBlock = false
                } else {
                    insideCodeBlock = true
                }
                continue
            }

            if (insideCodeBlock) {
                codeBlockBuilder.append(line).append("\n")
                continue
            }

            // Parse headers
            if (line.startsWith("#")) {
                val headerLevel = line.takeWhile { it == '#' }.length
                val headerText = line.drop(headerLevel).trim()
                Text(
                    text = headerText,
                    fontSize = (if (headerLevel == 1) 20.sp else if (headerLevel == 2) 17.sp else 15.sp) * sizeMultiplier,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                continue
            }

            // Parse lists
            if (line.trim().startsWith("-") || line.trim().startsWith("*")) {
                val listText = line.trim().drop(1).trim()
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "• ",
                        fontSize = 15.sp * sizeMultiplier,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = parseInlineStyles(listText),
                        fontSize = 14.sp * sizeMultiplier,
                        lineHeight = 20.sp * sizeMultiplier,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                continue
            }

            // Parse tables
            if (line.trim().startsWith("|") && line.contains("-")) {
                // Skip separator rows
                continue
            }
            if (line.trim().startsWith("|")) {
                val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    cells.forEach { cell ->
                        Text(
                            text = parseInlineStyles(cell),
                            fontSize = 12.sp * sizeMultiplier,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                        )
                    }
                }
                continue
            }

            // Normal text with inline styling
            if (line.trim().isNotEmpty()) {
                Text(
                    text = parseInlineStyles(line),
                    fontSize = 14.sp * sizeMultiplier,
                    lineHeight = 20.sp * sizeMultiplier,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Cleanup unclosed codeblock
        if (insideCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlockCard(code = codeBlockBuilder.toString().trim())
        }
    }
}

// Simple bold / monospace parser
fun parseInlineStyles(text: String) = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val nextBold = text.indexOf("**", index)
        val nextCode = text.indexOf("`", index)

        // Find which comes first
        val firstSpecial = when {
            nextBold != -1 && nextCode != -1 -> if (nextBold < nextCode) "bold" else "code"
            nextBold != -1 -> "bold"
            nextCode != -1 -> "code"
            else -> "none"
        }

        if (firstSpecial == "bold" && nextBold != -1) {
            append(text.substring(index, nextBold))
            val closingBold = text.indexOf("**", nextBold + 2)
            if (closingBold != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CyanPrimary)) {
                    append(text.substring(nextBold + 2, closingBold))
                }
                index = closingBold + 2
            } else {
                append("**")
                index = nextBold + 2
            }
        } else if (firstSpecial == "code" && nextCode != -1) {
            append(text.substring(index, nextCode))
            val closingCode = text.indexOf("`", nextCode + 1)
            if (closingCode != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Black.copy(alpha = 0.3f),
                        color = Color(0xFF81D4FA)
                    )
                ) {
                    append(text.substring(nextCode + 1, closingCode))
                }
                index = closingCode + 1
            } else {
                append("`")
                index = nextCode + 1
            }
        } else {
            append(text.substring(index))
            break
        }
    }
}

// --- TERMINAL STYLE CODE BLOCK COMPONENT ---
@Composable
fun CodeBlockCard(code: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E15)),
        border = BorderStroke(1.dp, Color(0xFF1E2132))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161722))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                }
                Text("CODE", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(
                    onClick = {
                        copyToClipboard(context, code)
                        Toast.makeText(context, "Code copié !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier", tint = TextGray, modifier = Modifier.size(12.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFE0E0E0),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: DISCUSSIONS (CONVERSATIONS HISTORY)
// ==========================================
@Composable
fun TabConversations(viewModel: JarvisViewModel, conversations: List<Conversation>, onMenuClick: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filteredList = conversations.filter {
        (it.title.contains(searchQuery, ignoreCase = true)) && !it.isArchived
    }

    val archivedList = conversations.filter {
        (it.title.contains(searchQuery, ignoreCase = true)) && it.isArchived
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu principal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "HISTORIQUE DES SESSIONS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search Conversations Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher une discussion...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                ),
                singleLine = true
            )
        }

        if (filteredList.isEmpty() && archivedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucune discussion trouvée",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (filteredList.isNotEmpty()) {
            item {
                Text("Discussions actives", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            itemsIndexed(filteredList) { index, chat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .framerMotionEntry(delayMillis = (index * 40).coerceAtMost(240))
                ) {
                    ConversationCard(
                        chat = chat,
                        activeId = viewModel.activeConversationId.value,
                        onSelect = {
                            viewModel.selectConversation(chat.id)
                            viewModel.currentTab = "accueil"
                        },
                        onDelete = { viewModel.deleteConversation(chat.id) },
                        onPin = { viewModel.togglePinConversation(chat.id) },
                        onArchive = { viewModel.toggleArchiveConversation(chat.id) },
                        onRenameTrigger = {
                            renameTargetId = chat.id
                            renameInput = chat.title
                        }
                    )
                }
            }
        }

        if (archivedList.isNotEmpty()) {
            item {
                Text("Discussions archivées", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
            }

            itemsIndexed(archivedList) { index, chat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .framerMotionEntry(delayMillis = (index * 40).coerceAtMost(240))
                ) {
                    ConversationCard(
                        chat = chat,
                        activeId = viewModel.activeConversationId.value,
                        onSelect = {
                            viewModel.selectConversation(chat.id)
                            viewModel.currentTab = "accueil"
                        },
                        onDelete = { viewModel.deleteConversation(chat.id) },
                        onPin = { viewModel.togglePinConversation(chat.id) },
                        onArchive = { viewModel.toggleArchiveConversation(chat.id) },
                        onRenameTrigger = {
                            renameTargetId = chat.id
                            renameInput = chat.title
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (renameTargetId != null) {
        AlertDialog(
            onDismissRequest = { renameTargetId = null },
            title = { Text("Renommer la discussion") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        renameTargetId?.let { id ->
                            viewModel.renameConversation(id, renameInput)
                        }
                        renameTargetId = null
                    }
                ) {
                    Text("Renommer")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetId = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun ConversationCard(
    chat: Conversation,
    activeId: String?,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onRenameTrigger: () -> Unit
) {
    val isActive = chat.id == activeId

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("conversation_card")
            .glassmorphic(
                borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                backgroundColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Épinglée",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = chat.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val formattedDate = SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()).format(Date(chat.createdAt))
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Operations Toolbar
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPin, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (chat.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Épingler",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = if (chat.isPinned) 1f else 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onArchive, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (chat.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = "Archiver",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onRenameTrigger, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Renommer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: TAB GENERATION (MULTIMODAL AI CENTER)
// ==========================================
@Composable
fun TabGeneration(viewModel: JarvisViewModel, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    var textInputState by remember { mutableStateOf(viewModel.textPromptInput) }
    var imageInputState by remember { mutableStateOf(viewModel.imagePromptInput) }
    var videoInputState by remember { mutableStateOf(viewModel.videoPromptInput) }
    var voiceInputState by remember { mutableStateOf(viewModel.voiceScriptInput) }

    // Sync state when changed externally
    LaunchedEffect(viewModel.imagePromptInput) {
        imageInputState = viewModel.imagePromptInput
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu principal",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "CENTRE DE CRÉATION MULTIMODALE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Horizontal Selection Chips for A, B, C, D options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(
                Pair("A", "Texte IA"),
                Pair("B", "Image IA"),
                Pair("D", "Voix Jarvis")
            )
            options.forEach { (code, label) ->
                val isSelected = viewModel.selectedGenOption == code
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedGenOption = code },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        val icon = when (code) {
                            "A" -> Icons.Rounded.Description
                            "B" -> Icons.Rounded.Image
                            "C" -> Icons.Rounded.Videocam
                            else -> Icons.Rounded.VolumeUp
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Render selected Option Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (viewModel.selectedGenOption) {
                    "A" -> OptionTextTab(viewModel, textInputState, onPromptChange = { textInputState = it })
                    "B" -> OptionImageTab(viewModel, imageInputState, onPromptChange = { imageInputState = it })
                    "C" -> OptionVideoTab(viewModel, videoInputState, onPromptChange = { videoInputState = it })
                    "D" -> OptionVoiceTab(viewModel, voiceInputState, onScriptChange = { voiceInputState = it })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun OptionTextTab(viewModel: JarvisViewModel, textInput: String, onPromptChange: (String) -> Unit) {
    val context = LocalContext.current
    val presets = listOf("Algorithme Python", "Rapport de Synthèse", "Poème de l'Espace", "Email Pro")
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Option A : Générateur de Texte", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Rédigez ou laissez Jarvis composer à l'aide de l'IA.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = textInput,
            onValueChange = {
                onPromptChange(it)
                viewModel.textPromptInput = it
            },
            placeholder = { Text("Écrivez votre prompt ou instruction...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Preset chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                SuggestionChip(
                    onClick = {
                        val prompt = when (preset) {
                            "Algorithme Python" -> "Rédige une fonction Python récursive optimisée pour trier des objets complexes par attribut."
                            "Rapport de Synthèse" -> "Rédige un compte rendu de réunion professionnelle sur la sécurité informatique."
                            "Poème de l'Espace" -> "Écris un poème néo-rétro futuriste sur un explorateur de trous noirs."
                            else -> "Rédige un mail de suivi poli mais assertif après un entretien d'embauche."
                        }
                        onPromptChange(prompt)
                        viewModel.textPromptInput = prompt
                    },
                    label = { Text(preset, fontSize = 11.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { viewModel.generateGenText() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isGeneratingText && textInput.trim().isNotEmpty()
        ) {
            if (viewModel.isGeneratingText) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthèse en cours...")
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lancer la Génération")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Result viewer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (viewModel.textGenResult.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RÉSULTAT", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis Text Gen", viewModel.textGenResult))
                                    Toast.makeText(context, "Texte copié !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            MarkdownTextViewer(text = viewModel.textGenResult, sizeMultiplier = viewModel.textSizeMultiplier)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Le texte généré s'affichera ici.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun OptionImageTab(viewModel: JarvisViewModel, imageInput: String, onPromptChange: (String) -> Unit) {
    val context = LocalContext.current
    val styles = listOf("Cyberpunk", "Fantasy", "Realist", "Anime", "3D")
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Option B : Générateur d'Images", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Créez de magnifiques illustrations IA via Pollinations.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = imageInput,
            onValueChange = {
                onPromptChange(it)
                viewModel.imagePromptInput = it
            },
            placeholder = { Text("Décrivez l'image à générer...") },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Preset styles
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            styles.forEach { style ->
                FilterChip(
                    selected = viewModel.imageStylePreset == style,
                    onClick = { viewModel.imageStylePreset = style },
                    label = { Text(style, fontSize = 11.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { viewModel.generateGenImage() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isGeneratingImage && imageInput.trim().isNotEmpty()
        ) {
            if (viewModel.isGeneratingImage) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calcul des vecteurs d'images...")
            } else {
                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Générer l'Image")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Image box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (viewModel.isGeneratingImage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Modélisation de la diffusion...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (viewModel.generatedImageUrl.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        coil.compose.AsyncImage(
                            model = viewModel.generatedImageUrl,
                            contentDescription = viewModel.imagePromptInput,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Download & Share Actions overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Image enregistrée dans la galerie (Simulation)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = "Télécharger", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Lien d'image partagé !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Partager", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else if (viewModel.imageGenerationError.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Échec de génération :",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.imageGenerationError,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("L'image générée s'affichera ici.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun OptionVideoTab(viewModel: JarvisViewModel, videoInput: String, onPromptChange: (String) -> Unit) {
    var fpsSelected by remember { mutableStateOf("60 FPS") }
    var cameraStyle by remember { mutableStateOf("Zoom") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Option C : Rendu de Vidéo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Générez des animations spatiales et tech en temps réel.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = videoInput,
            onValueChange = {
                onPromptChange(it)
                viewModel.videoPromptInput = it
            },
            placeholder = { Text("Décrivez le clip vidéo (ex : Espace, Neon, Nature)...") },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Video configs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val fpsOptions = listOf("24 FPS", "30 FPS", "60 FPS")
            val cameraOptions = listOf("Zoom", "Pan", "Orbit")
            
            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    fpsOptions.forEach { option ->
                        FilterChip(
                            selected = fpsSelected == option,
                            onClick = { fpsSelected = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    cameraOptions.forEach { option ->
                        FilterChip(
                            selected = cameraStyle == option,
                            onClick = { cameraStyle = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Button(
            onClick = { viewModel.generateGenVideo() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isGeneratingVideo && videoInput.trim().isNotEmpty()
        ) {
            if (viewModel.isGeneratingVideo) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calcul d'interpolarisation...")
            } else {
                Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthétiser la Vidéo")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Video monitor viewport or rendering status
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    backgroundColor = Color(0xFF070913).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (viewModel.isGeneratingVideo) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "QUANTUM RENDER ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { viewModel.videoGenerationProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.videoStatusLog,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (viewModel.videoResultReady) {
                    // Custom interactive procedural animation viewport representing the video loop
                    ProceduralVideoViewport(
                        prompt = videoInput,
                        cameraStyle = cameraStyle,
                        fps = fpsSelected,
                        frameUrls = viewModel.videoFrameUrls
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Le viewport de rendu s'affichera ici.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

// Procedural visual generator on Canvas representing the AI-generated video loop
@Composable
fun ProceduralVideoViewport(
    prompt: String,
    cameraStyle: String,
    fps: String,
    frameUrls: List<String> = emptyList()
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animate a phase representing progress in time
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )
    
    // 3 FPS Stop-motion index loop
    var activeFrameIndex by remember { mutableStateOf(0) }
    if (frameUrls.isNotEmpty()) {
        LaunchedEffect(frameUrls) {
            while (true) {
                delay(250) // Switch frame every 250ms
                activeFrameIndex = (activeFrameIndex + 1) % frameUrls.size
            }
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val lowerPrompt = prompt.lowercase(Locale.ROOT)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Render current video frame image from Pollinations if available!
        if (frameUrls.isNotEmpty() && activeFrameIndex < frameUrls.size) {
            coil.compose.AsyncImage(
                model = frameUrls[activeFrameIndex],
                contentDescription = "Flux vidéo généré",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        // Overlay Canvas with glowing futuristic particles, telemetry scanlines, and reticles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2
            
            // Draw grid overlay lines with some opacity
            val numLines = 6
            for (i in 0..numLines) {
                val y = i * (height / numLines)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            // Interactive drawing overlay
            if (frameUrls.isEmpty()) {
                // If no frame is generated, we draw the full screen high-tech abstract visuals
                when {
                    lowerPrompt.contains("espace") || lowerPrompt.contains("stars") || lowerPrompt.contains("space") -> {
                        // Space theme: Glowing revolving galaxies
                        val numStars = 60
                        for (i in 0 until numStars) {
                            val angle = (i * (360f / numStars)) * (Math.PI / 180f) + (phase * 0.15)
                            val radius = (15f + i * 4.5f) * (if (cameraStyle == "Zoom") 1f + sin(phase * 0.1f) else 1f)
                            val x = centerX + radius * cos(angle).toFloat()
                            val y = centerY + radius * sin(angle).toFloat()
                            
                            val sizeStar = 1.5f + (i % 3) * 1.5f
                            drawCircle(
                                color = if (i % 2 == 0) primaryColor.copy(alpha = 0.7f) else Color.White,
                                radius = sizeStar,
                                center = Offset(x, y)
                            )
                        }
                    }
                    lowerPrompt.contains("cyberpunk") || lowerPrompt.contains("tech") || lowerPrompt.contains("neon") -> {
                        // Cyberpunk wave style
                        val lines = 5
                        for (l in 0 until lines) {
                            val path = Path()
                            val offsetMultiplier = 20.dp.toPx()
                            path.moveTo(0f, centerY + sin(phase + l).toFloat() * offsetMultiplier)
                            for (x in 0..10) {
                                val px = (x / 10f) * width
                                val py = centerY + sin(phase + l + x * 0.6f).toFloat() * offsetMultiplier + (l * 12.dp.toPx() - 30.dp.toPx())
                                path.lineTo(px, py)
                            }
                            
                            drawPath(
                                path = path,
                                color = if (l % 2 == 0) primaryColor else secondaryColor,
                                style = Stroke(width = 1.5.dp.toPx()),
                                alpha = 0.6f
                            )
                        }
                    }
                    else -> {
                        // Default fluid vector circles
                        val circles = 6
                        for (c in 1..circles) {
                            val angle = phase * (if (c % 2 == 0) 1f else -1f) * (0.4f + c * 0.1f)
                            val radius = c * 24.dp.toPx() * (if (cameraStyle == "Zoom") 1.1f + sin(phase * 0.2f) * 0.1f else 1.0f)
                            val x = centerX + cos(angle).toFloat() * (c * 3.dp.toPx())
                            val y = centerY + sin(angle).toFloat() * (c * 3.dp.toPx())
                            
                            drawCircle(
                                color = primaryColor,
                                radius = radius,
                                center = Offset(x, y),
                                style = Stroke(width = 1.dp.toPx()),
                                alpha = (1f - (c / (circles * 1.1f))).coerceIn(0f, 1f) * 0.4f
                            )
                        }
                    }
                }
            } else {
                // If images are rendered, draw elegant neon target metrics overlay
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 35.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                // Outer bracket corners
                val bracketSize = 15.dp.toPx()
                val inset = 12.dp.toPx()
                
                // Top-Left corner bracket
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(inset, inset), Offset(inset + bracketSize, inset), 2.dp.toPx())
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(inset, inset), Offset(inset, inset + bracketSize), 2.dp.toPx())
                
                // Top-Right corner bracket
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(width - inset, inset), Offset(width - inset - bracketSize, inset), 2.dp.toPx())
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(width - inset, inset), Offset(width - inset, inset + bracketSize), 2.dp.toPx())
                
                // Bottom-Left corner bracket
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(inset, height - inset), Offset(inset + bracketSize, height - inset), 2.dp.toPx())
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(inset, height - inset), Offset(inset, height - inset - bracketSize), 2.dp.toPx())
                
                // Bottom-Right corner bracket
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(width - inset, height - inset), Offset(width - inset - bracketSize, height - inset), 2.dp.toPx())
                drawLine(primaryColor.copy(alpha = 0.5f), Offset(width - inset, height - inset), Offset(width - inset, height - inset - bracketSize), 2.dp.toPx())
                
                // Sweeping laser radar line
                val radarY = centerY + sin(phase).toFloat() * centerY
                drawLine(
                    color = secondaryColor.copy(alpha = 0.35f),
                    start = Offset(0f, radarY),
                    end = Offset(width, radarY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        
        // Metadata / Sci-fi overlay metrics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val cameraLabel = if (frameUrls.isNotEmpty()) "LOCKED [AI LOOP]" else "ACTIVE [$cameraStyle]"
            Text(
                "CAMERA: $cameraLabel\nFPS: $fps [STABLE]\nMODEL: JARVIS DIFFUSION v4",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            Text(
                "RENDER PORT: HD1080p\nCLIP COMPILATION ACTIVE (60fps)",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = primaryColor,
                modifier = Modifier.align(Alignment.BottomEnd),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun OptionVoiceTab(viewModel: JarvisViewModel, voiceInput: String, onScriptChange: (String) -> Unit) {
    val context = LocalContext.current
    val profiles = listOf("Jarvis", "Friday", "Cyber Synth", "Hologram")
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Option D : Générateur de Voix", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Convertissez du texte en répliques audio Jarvis personnalisées.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = voiceInput,
            onValueChange = {
                onScriptChange(it)
                viewModel.voiceScriptInput = it
            },
            placeholder = { Text("Entrez la réplique que l'IA doit dicter...") },
            modifier = Modifier.fillMaxWidth().height(65.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Profiles selector
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            profiles.forEach { profile ->
                FilterChip(
                    selected = viewModel.selectedVoiceProfile == profile,
                    onClick = { viewModel.selectedVoiceProfile = profile },
                    label = { Text(profile, fontSize = 11.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Speed & Pitch Sliders
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hauteur (Pitch) : x${"%.2f".format(viewModel.voicePitchMultiplier)}", fontSize = 11.sp, color = Color.White)
                Slider(
                    value = viewModel.voicePitchMultiplier,
                    onValueChange = { viewModel.voicePitchMultiplier = it },
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.width(180.dp).height(20.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vitesse (Rate) : x${"%.2f".format(viewModel.voiceRateMultiplier)}", fontSize = 11.sp, color = Color.White)
                Slider(
                    value = viewModel.voiceRateMultiplier,
                    onValueChange = { viewModel.voiceRateMultiplier = it },
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.width(180.dp).height(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { viewModel.generateGenVoice() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isGeneratingVoice && voiceInput.trim().isNotEmpty()
        ) {
            if (viewModel.isGeneratingVoice) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modulation de fréquence...")
            } else {
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthétiser la Voix")
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Voice visualization box with standard canvas drawing sine waves
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    backgroundColor = Color(0xFF060913).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "FRÉQUENCE AUDIO DE SYNTHÈSE",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Waveform active or idle canvas
                    LiveAudioWaveform(isActive = viewModel.isGeneratingVoice || viewModel.isSpeakingTts)
                }
            }
        }
    }
}

// Sine wave oscilloscope drawn dynamically on Canvas
@Composable
fun LiveAudioWaveform(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw 3 layers of sine waves with different speeds & amplitudes
        val numWaves = 3
        for (w in 0 until numWaves) {
            val path = Path()
            val amp = if (isActive) (20.dp.toPx() - w * 4.dp.toPx()) else 3.dp.toPx()
            val frequency = if (isActive) (0.015f + w * 0.005f) else 0.008f
            val speed = if (isActive) (phase * (1f + w * 0.5f)) else (phase * 0.2f)
            
            path.moveTo(0f, centerY)
            for (x in 0..width.toInt() step 5) {
                val dx = x.toFloat()
                val dy = centerY + sin(dx * frequency + speed).toFloat() * amp
                path.lineTo(dx, dy)
            }
            
            drawPath(
                path = path,
                color = if (w == 0) primaryColor else if (w == 1) secondaryColor else Color.White,
                style = Stroke(width = (2.dp.toPx() - w * 0.5.dp.toPx())),
                alpha = if (isActive) (0.8f - w * 0.2f) else 0.25f
            )
        }
        
        // Center line (grid)
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
    }
}

// ==========================================
// SCREEN 4: GESTION DES FICHIERS
// ==========================================
@Composable
fun TabFichiers(viewModel: JarvisViewModel, files: List<UploadedFile>, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCreatingSimDoc by remember { mutableStateOf(false) }

    // Sim Document Creator state
    var simDocName by remember { mutableStateOf("") }
    var simDocMime by remember { mutableStateOf("text/plain") }
    var simDocContent by remember { mutableStateOf("") }

    // File Import launcher (real device storage text/images)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val mimeType = context.contentResolver.getType(uri) ?: "text/plain"
                val name = uri.lastPathSegment ?: "document_importé"
                
                inputStream?.use { stream ->
                    val bytes = stream.readBytes()
                    val sizeKb = "${bytes.size / 1024} KB"
                    
                    // If it is an image, convert to Base64
                    val contentString = if (mimeType.startsWith("image/")) {
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        // Text reading
                        String(bytes)
                    }

                    viewModel.saveUploadedFile(name, mimeType, sizeKb, contentString)
                    Toast.makeText(context, "Fichier '$name' importé !", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur d'importation : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu principal",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "GESTIONNAIRE DE DOCUMENTS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Upload Button controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { fileLauncher.launch("*/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importer", fontSize = 12.sp)
            }

            Button(
                onClick = { isCreatingSimDoc = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.NoteAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer Doc", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preloaded futuristic analysis templates for immediate testing!
        Text(
            "Modèles d'analyse rapide (Recommandé)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickTemplateCard(
                name = "Rapport Financier PDF",
                type = "application/pdf",
                size = "45 KB",
                onClick = {
                    viewModel.saveUploadedFile(
                        "Rapport_Financier_JarvisCorp.pdf",
                        "application/pdf",
                        "45 KB",
                        "--- RAPPORT DE RENTABILITÉ Q1 2026 JARVISCORP ---\n" +
                                "Chiffre d'Affaires Brut : 12,400,000 EUR (+15% YoY)\n" +
                                "Marge Opérationnelle : 4,200,000 EUR\n" +
                                "Coûts de Recherche IA : 2,100,000 EUR\n" +
                                "Trésorerie Disponible : 8,500,000 EUR\n" +
                                "Conclusion : La division IA 'Friday' est en forte croissance. Rentabilité attendue pour Q3."
                    )
                    Toast.makeText(context, "Modèle PDF financier généré !", Toast.LENGTH_SHORT).show()
                }
            )

            QuickTemplateCard(
                name = "Base de données CSV",
                type = "text/csv",
                size = "12 KB",
                onClick = {
                    viewModel.saveUploadedFile(
                        "Inventaire_Serveurs_Host.csv",
                        "text/csv",
                        "12 KB",
                        "HostID,HostName,CPU_Cores,RAM_GB,Status,PingMs\n" +
                                "1,Jarvis-Core-Main,128,1024,ONLINE,1ms\n" +
                                "2,Jarvis-Secondary-Db,64,512,ONLINE,3ms\n" +
                                "3,Backup-Node-01,32,256,OFFLINE,999ms\n" +
                                "4,Friday-Engine,64,512,ONLINE,2ms"
                    )
                    Toast.makeText(context, "Modèle CSV d'inventaire généré !", Toast.LENGTH_SHORT).show()
                }
            )

            QuickTemplateCard(
                name = "Script Python.py",
                type = "text/plain",
                size = "8 KB",
                onClick = {
                    viewModel.saveUploadedFile(
                        "analytics_engine.py",
                        "text/plain",
                        "8 KB",
                        "import numpy as np\n" +
                                "import pandas as pd\n\n" +
                                "def analyze_latency(ping_list):\n" +
                                "    \"\"\"Calcule la latence moyenne de Jarvis\"\"\"\n" +
                                "    clean_pings = [p for p in ping_list if p < 500]\n" +
                                "    mean_lat = np.mean(clean_pings)\n" +
                                "    print(f'Latence moyenne active: {mean_lat}ms')\n" +
                                "    return mean_lat"
                    )
                    Toast.makeText(context, "Modèle de script Python généré !", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Vos Documents Importés",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        // Uploaded files list
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aucun document chargé dans le système.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(files) { file ->
                    FileRowCard(
                        file = file,
                        isSelectedForAttachment = viewModel.selectedAttachedFile?.id == file.id,
                        onAttachToggle = {
                            if (viewModel.selectedAttachedFile?.id == file.id) {
                                viewModel.selectedAttachedFile = null
                            } else {
                                viewModel.selectedAttachedFile = file
                                viewModel.currentTab = "accueil"
                                Toast.makeText(context, "'${file.name}' joint au chat !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = { viewModel.deleteUploadedFile(file.id) }
                    )
                }
            }
        }
    }

    // Interactive simulated creator modal
    if (isCreatingSimDoc) {
        AlertDialog(
            onDismissRequest = { isCreatingSimDoc = false },
            title = { Text("Créer un document texte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = simDocName,
                        onValueChange = { simDocName = it },
                        placeholder = { Text("Nom du fichier (ex: notes.txt)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = simDocContent,
                        onValueChange = { simDocContent = it },
                        placeholder = { Text("Écrivez le contenu du document...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (simDocName.trim().isNotEmpty() && simDocContent.trim().isNotEmpty()) {
                            viewModel.saveUploadedFile(
                                simDocName,
                                simDocMime,
                                "${simDocContent.length / 100} KB",
                                simDocContent
                            )
                            isCreatingSimDoc = false
                            simDocName = ""
                            simDocContent = ""
                        }
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreatingSimDoc = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun QuickTemplateCard(name: String, type: String, size: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .glassmorphic(
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = if (type.startsWith("image")) Icons.Rounded.Image else Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$type • $size", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FileRowCard(
    file: UploadedFile,
    isSelectedForAttachment: Boolean,
    onAttachToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(
                borderColor = if (isSelectedForAttachment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                backgroundColor = if (isSelectedForAttachment) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.mimeType.startsWith("image/")) Icons.Rounded.Image else Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${file.mimeType} • ${file.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAttachToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isSelectedForAttachment) Icons.Rounded.Attachment else Icons.Outlined.Attachment,
                        contentDescription = "Joindre au chat",
                        tint = if (isSelectedForAttachment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: CONFIGURATION / PARAMÈTRES
// ==========================================
@Composable
fun TabParametres(viewModel: JarvisViewModel, memoryItems: List<MemoryItem>, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    var isAddingMemory by remember { mutableStateOf(false) }
    var memoryInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu principal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CONFIGURATION DU SYSTÈME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- SECTION 1: PROFIL DE L'UTILISATEUR ---
        item {
            SettingsSectionTitle("Identité de l'Utilisateur")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.userName,
                        onValueChange = { viewModel.updateUserNameSetting(it) },
                        label = { Text("Votre prénom ou titre honorifique") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.userPreferences,
                        onValueChange = { viewModel.updateUserPreferencesSetting(it) },
                        label = { Text("Directives comportementales de Jarvis") },
                        placeholder = { Text("Ex: Jarvis, sois poli, précis et appelle-moi Patron.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }
        }

        // --- SECTION 1.5: CONFIGURATION DE LA CLÉ API ---
        item {
            SettingsSectionTitle("Configuration de la Clé API Gemini")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Si l'application n'arrive pas à se connecter à l'API Gemini par défaut, vous pouvez entrer votre propre clé API ci-dessous. Elle sera enregistrée de manière sécurisée et locale dans votre appareil.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var isKeyVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = viewModel.geminiApiKey,
                        onValueChange = { viewModel.updateGeminiApiKey(it) },
                        label = { Text("Clé API Gemini (GEMINI_API_KEY)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isKeyVisible) "Masquer la clé" else "Afficher la clé"
                                )
                            }
                        }
                    )

                    if (viewModel.geminiApiKey.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Clé personnalisée active (Prend le dessus sur les secrets)",
                                fontSize = 11.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Vide : Utilise la clé par défaut injectée à la compilation",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 2: GESTION DE LA MÉMOIRE ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SettingsSectionTitle("Mémoire de Jarvis")
                Switch(
                    checked = viewModel.isMemoryEnabled,
                    onCheckedChange = { viewModel.toggleMemorySetting(it) }
                )
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Grâce à sa mémoire, Jarvis retient vos préférences, vos centres d'intérêt et vos informations importantes de manière permanente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (viewModel.isMemoryEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isAddingMemory = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer un nouveau souvenir")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (memoryItems.isEmpty()) {
                            Text(
                                "La mémoire de Jarvis est actuellement vide.",
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Souvenirs enregistrés :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            memoryItems.forEach { memory ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = memory.content,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteMemoryItem(memory.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: PARAMÈTRES AUDIO & SERVICES ---
        item {
            SettingsSectionTitle("Services et Réponses")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Lecture vocale des réponses", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Jarvis lira ses réponses à haute voix", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = viewModel.ttsEnabled, onCheckedChange = { viewModel.toggleTtsSetting(it) })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Recherche Web automatique", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Permet d'extraire des infos récentes d'Internet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = viewModel.webSearchEnabled, onCheckedChange = { viewModel.toggleWebSearchSetting(it) })
                    }
                }
            }
        }

        // --- SECTION 4: THÈME & APPARENCE ---
        item {
            SettingsSectionTitle("Personnalisation Visuelle")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Accent color picker
                    Column {
                        Text("Couleur d'accentuation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val colorsList = listOf(CyanPrimary, OrangePrimary, PurpleAccent, GreenPrimary, GoldPrimary)
                            colorsList.forEachIndexed { idx, color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (viewModel.accentColorIndex == idx) 3.dp else 1.dp,
                                            color = if (viewModel.accentColorIndex == idx) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.updateAccentColorSetting(idx) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Theme selector
                    Column {
                        Text("Mode d'affichage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val modes = listOf("dark" to "Sombre", "light" to "Clair", "system" to "Système")
                            modes.forEach { (mode, label) ->
                                OutlinedButton(
                                    onClick = { viewModel.updateThemeSetting(mode) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (viewModel.themeMode == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (viewModel.themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (viewModel.themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Text size scale
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Taille de texte", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${(viewModel.textSizeMultiplier * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = viewModel.textSizeMultiplier,
                            onValueChange = { viewModel.updateTextSizeSetting(it) },
                            valueRange = 0.8f..1.4f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // --- SECTION 5: SÉCURITÉ & DONNÉES ---
        item {
            SettingsSectionTitle("Sécurité et Données")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.clearAllConversations()
                            Toast.makeText(context, "Discussions effacées !", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Supprimer toutes les discussions", color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllMemory()
                            Toast.makeText(context, "Mémoire réinitialisée !", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Effacer la mémoire permanente")
                    }
                }
            }
        }

        // --- APP META CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "JARVIS ASSISTANT IA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 2.6.0-Premium (GitHub Production Build)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Conçu en Kotlin et Jetpack Compose avec l'API Gemini 3.5-Flash.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }

    // Add Memory Dialog
    if (isAddingMemory) {
        AlertDialog(
            onDismissRequest = { isAddingMemory = false },
            title = { Text("Enregistrer un souvenir") },
            text = {
                OutlinedTextField(
                    value = memoryInput,
                    onValueChange = { memoryInput = it },
                    placeholder = { Text("Ex: J'adore le café noir serré.") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memoryInput.trim().isNotEmpty()) {
                            viewModel.addMemoryItem(memoryInput)
                            isAddingMemory = false
                            memoryInput = ""
                            Toast.makeText(context, "Souvenir mémorisé !", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Mémoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingMemory = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
    )
}

// ==========================================
// HIGH FIDELITY HOLO-GLOW SPHERE CANVAS
// ==========================================
@Composable
fun GlowingSphere(
    state: String, // "idle", "listening", "thinking", "speaking"
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition()

    // 1. Slow Pulse scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    "listening" -> 500
                    "thinking" -> 300
                    "speaking" -> 700
                    else -> 1800 // idle
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 2. Slow Orbit Rotation animation
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    "thinking" -> 1500
                    "listening" -> 6000
                    else -> 12000
                },
                easing = LinearEasing
            )
        )
    )

    // 3. Floating offset animation
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                translationY = floatY
            }
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)
        val radius = width.coerceAtMost(height) / 2.2f

        // --- LAYER 1: Radial Glow Background ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (state == "listening") 0.45f else 0.25f),
                    primaryColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.5f
            )
        )

        // --- LAYER 2: Concentric ripples for "speaking" / soundwaves ---
        if (state == "speaking" || state == "listening") {
            val rippleCount = 3
            for (i in 1..rippleCount) {
                val phaseOffset = (System.currentTimeMillis() % 2000) / 2000f
                val sizeRatio = ((i + phaseOffset) / rippleCount) % 1.0f
                drawCircle(
                    color = primaryColor.copy(alpha = (1.0f - sizeRatio) * 0.25f),
                    radius = radius * (1.0f + sizeRatio * 0.8f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // --- LAYER 3: Core Holographic Spherical Orbits ---
        // Rotating orbits with specific tilts
        val numOrbits = if (state == "thinking") 4 else 3
        for (i in 0 until numOrbits) {
            val tiltAngle = (45f + i * 50f)
            val radTilt = Math.toRadians(tiltAngle.toDouble())

            val angleRad = Math.toRadians(orbitRotation.toDouble() * (if (i % 2 == 0) 1 else -1))

            // Build dynamic ellipse path on canvas
            val path = Path()
            val steps = 80
            for (step in 0..steps) {
                val t = (2 * Math.PI * step) / steps
                // Ellipse calculation
                val rx = radius * 1.0f
                val ry = radius * (if (state == "thinking") 0.45f else 0.25f)

                // Rotated ellipse coordinates
                val xOrig = rx * cos(t)
                val yOrig = ry * sin(t)

                val xRot = xOrig * cos(radTilt) - yOrig * sin(radTilt) + center.x
                val yRot = xOrig * sin(radTilt) + yOrig * cos(radTilt) + center.y

                if (step == 0) {
                    path.moveTo(xRot.toFloat(), yRot.toFloat())
                } else {
                    path.lineTo(xRot.toFloat(), yRot.toFloat())
                }
            }
            path.close()

            drawPath(
                path = path,
                color = if (i % 2 == 0) primaryColor.copy(alpha = 0.45f) else secondaryColor.copy(alpha = 0.35f),
                style = Stroke(
                    width = (if (state == "thinking") 2.dp else 1.2.dp).toPx()
                )
            )

            // Draw small orbiting tech-beads on the orbits
            val rx = radius * 1.0f
            val ry = radius * (if (state == "thinking") 0.45f else 0.25f)
            val beadX = rx * cos(angleRad)
            val beadY = ry * sin(angleRad)
            val beadXRot = beadX * cos(radTilt) - beadY * sin(radTilt) + center.x
            val beadYRot = beadX * sin(radTilt) + beadY * cos(radTilt) + center.y

            drawCircle(
                color = primaryColor,
                radius = 3.5.dp.toPx(),
                center = Offset(beadXRot.toFloat(), beadYRot.toFloat())
            )
        }

        // --- LAYER 4: Solid core power orb ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * (if (state == "thinking") 0.4f else 0.35f)
            )
        )
    }
}

// Custom Easing for breathing pulse wave
val SineWaveEasing = Easing { fraction ->
    sin(fraction * Math.PI * 2f).toFloat() / 2f + 0.5f
}

// --- UTILITIES ---
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Jarvis Message", text)
    clipboard.setPrimaryClip(clip)
}

// --- SHIMMER SKELETON LOADERS ---
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(4.dp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.6f)
    val highlightColor = if (isDark) Color(0xFF334155).copy(alpha = 0.7f) else Color(0xFFF1F5F9).copy(alpha = 0.8f)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 300f, 300f)
    )

    this.clip(shape).background(brush = brush)
}

@Composable
fun JarvisMessageSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .shimmer(shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .shimmer(shape = RoundedCornerShape(4.dp))
            )
        }

        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.85f)
                .padding(start = 8.dp),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Headline skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .shimmer(shape = RoundedCornerShape(6.dp))
                )
                
                // Content line 1 skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(12.dp)
                        .shimmer(shape = RoundedCornerShape(4.dp))
                )
                
                // Content line 2 skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .shimmer(shape = RoundedCornerShape(4.dp))
                )
                
                // Content line 3 skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .shimmer(shape = RoundedCornerShape(4.dp))
                )
                
                // Bottom small loader with text
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .shimmer(shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jarvis analyse & formule...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WebSearchSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Status Bar skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .shimmer(shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Interrogation des moteurs de recherche...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Simulated search result logs (repeat 3 times)
        repeat(3) { index ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title and Icon header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .shimmer(shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Query line
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(14.dp)
                                    .shimmer(shape = RoundedCornerShape(4.dp))
                            )
                            // Timestamp line
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(10.dp)
                                    .shimmer(shape = RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    // Content snippet lines
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(11.dp)
                                .shimmer(shape = RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(11.dp)
                                .shimmer(shape = RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(11.dp)
                                .shimmer(shape = RoundedCornerShape(4.dp))
                        )
                    }

                    // Source pills skeleton
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(24.dp)
                                    .shimmer(shape = RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
