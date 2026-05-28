package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs

// ViewModel to manage timer logic across screens and retain state on rotation
class TimerViewModel : ViewModel() {
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished = _isFinished.asStateFlow()

    private var timerJob: Job? = null
    var totalDurationSeconds: Long = 0L

    fun startTimer(h: Int, m: Int, s: Int) {
        totalDurationSeconds = (h * 3600L) + (m * 60L) + s
        if (totalDurationSeconds <= 0) return
        _remainingSeconds.value = totalDurationSeconds
        _isFinished.value = false
        resumeTimer()
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_remainingSeconds.value <= 0) {
            _isFinished.value = true
            return
        }
        _isRunning.value = true
        _isFinished.value = false
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            _isRunning.value = false
            _isFinished.value = true
        }
    }

    fun cancelTimer() {
        pauseTimer()
        _remainingSeconds.value = 0
        _isFinished.value = false
    }

    fun resetAlarm() {
        _isFinished.value = false
        _remainingSeconds.value = 0
    }

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val timerViewModel: TimerViewModel = viewModel()

            // Root dark background container conforming to Edge to Edge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF09090A))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "picker",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("picker") {
                        TimerPickerScreen(
                            viewModel = timerViewModel,
                            onStart = { h, m, s ->
                                timerViewModel.startTimer(h, m, s)
                                navController.navigate("active")
                            }
                        )
                    }
                    composable("active") {
                        ActiveTimerScreen(
                            viewModel = timerViewModel,
                            onCancel = {
                                timerViewModel.cancelTimer()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerPickerScreen(
    viewModel: TimerViewModel,
    onStart: (Int, Int, Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val totalInputSeconds = (hours * 3600) + (minutes * 60) + seconds
    val isStartEnabled = totalInputSeconds > 0

    // Premium dark-nebula radial gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0B10),
                        Color(0xFF060507)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            // Landscape Layout: Header, PickerLayout, and Footer are vertically stacked and aligned with the wheels on the left, StartButton is on the right.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    // Header Typography
                    Text(
                        text = "Timer (IOS-Standby Style)",
                        color = Color(0xFFFFBF00),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Spin wheels to set countdown duration",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    PickerLayout(
                        onHChange = { hours = it },
                        onMChange = { minutes = it },
                        onSChange = { seconds = it },
                        modifier = Modifier.wrapContentWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Selected duration footer aligned with wheels
                    Text(
                        text = "Selected: ${String.format("%02dh %02dm %02ds", hours, minutes, seconds)}",
                        color = if (isStartEnabled) Color(0xFFFFBF00) else Color.White.copy(alpha = 0.25f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))

                StartButton(
                    isEnabled = isStartEnabled,
                    onClick = { onStart(hours, minutes, seconds) }
                )
            }
        } else {
            // Portrait Layout: Stacked top-to-bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Typography
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Timer (IOS-Standby Style)",
                        color = Color(0xFFFFBF00),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Spin wheels to set countdown duration",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    PickerLayout(
                        onHChange = { hours = it },
                        onMChange = { minutes = it },
                        onSChange = { seconds = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(56.dp))
                    StartButton(
                        isEnabled = isStartEnabled,
                        onClick = { onStart(hours, minutes, seconds) }
                    )
                }

                // Beautiful footer status feedback
                Text(
                    text = "Selected: ${String.format("%02dh %02dm %02ds", hours, minutes, seconds)}",
                    color = if (isStartEnabled) Color(0xFFFFBF00) else Color.White.copy(alpha = 0.25f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PickerLayout(
    onHChange: (Int) -> Unit,
    onMChange: (Int) -> Unit,
    onSChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(260.dp)
    ) {
        // Aesthetic selector highlight capsule overlay (glassmorphic)
        Surface(
            modifier = Modifier
                .height(64.dp)
                .width(280.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, Color(0xFFFFBF00).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(32.dp)
        ) {}

        Row(
            modifier = Modifier.width(270.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                range = 0..23,
                label = "h",
                onSelect = onHChange,
                modifier = Modifier.testTag("picker_hours")
            )
            WheelPicker(
                range = 0..59,
                label = "m",
                onSelect = onMChange,
                modifier = Modifier.testTag("picker_minutes")
            )
            WheelPicker(
                range = 0..59,
                label = "s",
                onSelect = onSChange,
                modifier = Modifier.testTag("picker_seconds")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    range: IntRange,
    label: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    
    // Use an adequate large cyclic number instead of Int.MAX_VALUE to prevent snapping overflows and index out of bounds
    val totalItemsCount = 10000
    val itemsSize = if (items.isNotEmpty()) items.size else 1
    val initialIndex = remember { (totalItemsCount / 2 / itemsSize) * itemsSize }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snappingLayout = rememberSnapFlingBehavior(listState)
    
    // Store selected index to cleanly highlight active item without reading layoutInfo in composition
    var selectedIndex by remember { mutableIntStateOf(initialIndex % itemsSize) }
    
    // Support physical feedback whenever the selected element changes
    val haptic = LocalHapticFeedback.current
    
    // Keep updated callback state to prevent restart of LaunchedEffect
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Mathematically resolve center selected item with snapshotFlow. 
    // This only fires callbacks on actual integer index boundary changes (distinctUntilChanged), removing scroll lags.
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty() && items.isNotEmpty()) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val closestItem = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2f
                    abs(itemCenter - viewportCenter)
                }
                closestItem?.index?.let { it % items.size }
            } else {
                null
            }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .collect { resolvedIndex ->
            if (selectedIndex != resolvedIndex) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            selectedIndex = resolvedIndex
            if (items.isNotEmpty() && resolvedIndex in items.indices) {
                currentOnSelect(items[resolvedIndex])
            }
        }
    }

    Box(
        modifier = modifier
            .height(240.dp)
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snappingLayout,
            contentPadding = PaddingValues(vertical = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(totalItemsCount) { i ->
                val item = if (items.isNotEmpty()) items[i % items.size] else 0
                val isSelected = selectedIndex == (i % itemsSize)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            // Run fully deferred inside graphicsLayer to ensure zero composition-time feedback issues
                            val layoutInfo = listState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val itemInfo = visibleItems.find { itemInfo -> itemInfo.index == i }
                            val opacity = if (itemInfo != null) {
                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val maxDistance = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                                if (maxDistance <= 0f) {
                                    1f
                                } else {
                                    val distance = abs(itemCenter - viewportCenter)
                                    val fraction = distance / maxDistance
                                    if (fraction.isNaN()) 1f else (1f - fraction).coerceIn(0f, 1f)
                                }
                            } else {
                                0f
                            }

                            val scale = opacity * 0.4f + 0.75f
                            val rotationAngle = (1f - opacity) * -45f

                            alpha = opacity.coerceIn(0.12f, 1f)
                            scaleX = scale
                            scaleY = scale
                            rotationX = rotationAngle
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d%s", item, label),
                        color = if (isSelected) Color(0xFFFFBF00) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StartButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StartButtonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val scale = if (isEnabled) pulseScale else 1f

    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFBF00),
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFFFFBF00).copy(alpha = 0.12f),
            disabledContentColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("start_button"),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Text(
            text = "START",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun ActiveTimerScreen(
    viewModel: TimerViewModel,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val remaining by viewModel.remainingSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    // Immersive Edge-to-Edge Force Landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Keep Screen Awake (Screen On flag) while active timer is running and not finished
    DisposableEffect(isFinished) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            if (!isFinished) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val progress = if (viewModel.totalDurationSeconds > 0) {
        remaining.toFloat() / viewModel.totalDurationSeconds
    } else 0f

    // Flashing Alert Pulse for times-up screen activity
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmFlash")
    val alertScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlertScale"
    )

    val alertColorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlertAlpha"
    )

    // Ensure the whole screen blinks amber/yellow color on times-up as requested
    val screenBgColor = if (isFinished) {
        Color(0xFFFFBF00).copy(alpha = alertColorAlpha)
    } else {
        Color.Black
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        if (isFinished) {
            // Alarm Status Finish View (high contrast full screen blink)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alarm Active",
                        tint = Color(0xFFFFBF00),
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer {
                                scaleX = alertScale
                                scaleY = alertScale
                            }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TIME'S UP!",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.resetAlarm()
                            onCancel()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFBF00),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dismiss_alarm_button")
                    ) {
                        Text(
                            text = "DISMISS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else {
            // Active countdown layout with masked dual-layer color inversion
            
            // Layer 1: Base Layer (Black background, White countdown text)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                ActiveTimerContent(
                    isRunning = isRunning,
                    remaining = remaining,
                    textColor = Color.White,
                    controlColor = Color.White,
                    buttonBgColor = Color.White.copy(alpha = 0.08f),
                    onPauseResume = {
                        if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                    },
                    onCancel = {
                        viewModel.cancelTimer()
                        onCancel()
                    },
                    viewModel = viewModel
                )
            }

            // Layer 2: Clipped Overlay Layer (Yellow progress background, Black countdown text)
            val progressClipShape = remember(progress) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        return Outline.Rectangle(Rect(0f, 0f, size.width * progress, size.height))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = progressClipShape
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF8C00), Color(0xFFFFDF00))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.Center
                ) {
                    ActiveTimerContent(
                        isRunning = isRunning,
                        remaining = remaining,
                        textColor = Color.Black,
                        controlColor = Color.Black,
                        buttonBgColor = Color.Black.copy(alpha = 0.15f),
                        onPauseResume = {
                            if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                        },
                        onCancel = {
                            viewModel.cancelTimer()
                            onCancel()
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTimerContent(
    isRunning: Boolean,
    remaining: Long,
    textColor: Color,
    controlColor: Color,
    buttonBgColor: Color,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Control Column Side: moved slightly to the left with an offset
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .offset(x = (-24).dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Control Action Button
                TimerControlButton(
                    icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause Timer" else "Resume Timer",
                    tag = "pause_resume_button",
                    tint = controlColor,
                    bgColor = buttonBgColor,
                    onClick = onPauseResume
                )

                // Cancel / Exit Screen action
                TimerControlButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Cancel Timer",
                    tag = "cancel_timer_button",
                    tint = controlColor,
                    bgColor = buttonBgColor,
                    onClick = onCancel
                )
            }
        }

        // Typography Output Time Numbers
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.3f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "TIMER REMAINING",
                color = if (textColor == Color.Black) Color.Black else Color(0xFFFFBF00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = viewModel.formatTime(remaining),
                color = if (textColor == Color.White && remaining <= 5) Color(0xFFFF3B30) else textColor,
                fontSize = 140.sp, // Enlarged font size
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-2).sp,
                modifier = Modifier.testTag("active_timer_display")
            )
        }
    }
}

@Composable
fun TimerControlButton(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    tint: Color = Color.White,
    bgColor: Color = Color.White.copy(alpha = 0.08f),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.15f)),
        modifier = Modifier
            .size(72.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
