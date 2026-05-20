package com.prexoft.replygpt

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prexoft.replygpt.ui.theme.ReplyGPTTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        val preferencesManager = PreferencesManager(this)

        setContent {
            ReplyGPTTheme(darkTheme = true) {
                MainApp(preferencesManager)
            }
        }
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@Composable
fun MainApp(preferencesManager: PreferencesManager) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var checkedPackages by remember { mutableStateOf(preferencesManager.getEnabledPackages()) }
    var searchQuery by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showUserInfoDialog by remember { mutableStateOf(false) }
    var dailyReplyCount by remember { mutableStateOf(0) }
    var weeklyReplyCounts by remember { mutableStateOf(listOf<Int>()) }

    fun isNotificationServiceEnabled() : Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    fun requestNotificationPermission() {
        Toast.makeText(context, "Please enable notification access", Toast.LENGTH_SHORT).show()
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        context.startActivity(intent)
    }

    fun loadData() {
        dailyReplyCount = preferencesManager.getDailyReplyCount()
        weeklyReplyCounts = preferencesManager.getWeeklyReplyCounts()
    }

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)
        apps = resolveInfoList.filter { resolveInfo ->
            val flags = resolveInfo.activityInfo.applicationInfo.flags
            (flags and ApplicationInfo.FLAG_SYSTEM) == 0 && (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        }.map { resolveInfo ->
            AppInfo(
                name = resolveInfo.loadLabel(context.packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(context.packageManager)
            )
        }.sortedBy { it.name }
        loadData()
    }

    LaunchedEffect(LocalView.current) {
        loadData()
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showUserInfoDialog) {
        UserInfoDialog(
            initialValue = preferencesManager.getUserInfo(),
            onDismiss = { showUserInfoDialog = false },
            onSave = {
                preferencesManager.setUserInfo(it)
                showUserInfoDialog = false
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { i->
        Column(
            modifier = Modifier
                .padding(0.dp, i.calculateTopPadding(), 0.dp, 0.dp)
                .fillMaxSize()
        ) {
            NotificationGraphStart(count = dailyReplyCount, weeklyCounts = weeklyReplyCounts)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp, 16.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search apps...", color = Color.White.copy(0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White.copy(0.7f)
                    ),
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true
                )
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Menu", tint = Color.White.copy(0.6f))
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("User info") },
                            onClick = {
                                showUserInfoDialog = true
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Enable all") },
                            onClick = {
                                if (isNotificationServiceEnabled()) {
                                    val allPackages = apps.map { it.packageName }.toSet()
                                    checkedPackages = allPackages
                                    preferencesManager.setEnabledPackages(allPackages)
                                }
                                else requestNotificationPermission()
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Disable all") },
                            onClick = {
                                checkedPackages = emptySet()
                                preferencesManager.setEnabledPackages(emptySet())
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            }
            AppList(
                apps = filteredApps,
                checkedPackages = checkedPackages,
                onPackageChecked = { pkg, isChecked ->
                    if (isChecked && !isNotificationServiceEnabled()) {
                        requestNotificationPermission()
                    }
                    else {
                        val newSet = if (isChecked) checkedPackages + pkg else checkedPackages - pkg
                        checkedPackages = newSet
                        preferencesManager.setEnabledPackages(newSet)
                    }
                }
            )
        }
    }
}

@Composable
fun AppList(
    apps: List<AppInfo>,
    checkedPackages: Set<String>,
    onPackageChecked: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val showTopFade by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    val showBottomFade by remember { derivedStateOf { listState.canScrollForward } }
    val view = LocalView.current

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                val threshold = 100

                if (currentIndex != lastIndex) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastIndex = currentIndex
                    lastOffset = currentOffset
                }
                else if (abs(currentOffset - lastOffset) > threshold) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastOffset = currentOffset
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(apps) {
                AppItem(
                    app = it,
                    isChecked = checkedPackages.contains(it.packageName),
                    onCheckedChange = { checked -> onPackageChecked(it.packageName, checked) }
                )
            }
        }

        AnimatedVisibility(
            visible = showTopFade,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = showBottomFade,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.1f))
            .clickable { onCheckedChange(!isChecked) }
            .padding(12.dp, 0.dp, 6.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = app.icon,
            contentDescription = app.name,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = isChecked,
            modifier = Modifier.scale(0.7f),
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF03A9F4), // blue
                uncheckedThumbColor = Color.White.copy(0.4f),
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color.White.copy(0.2f),
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun NotificationGraphStart(count: Int, weeklyCounts: List<Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Replies today...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(0.8f)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val width = size.width
                val height = size.height
                val path = Path()

                if (weeklyCounts.isNotEmpty()) {
                    val maxVal = (weeklyCounts.maxOrNull() ?: 1).toFloat().coerceAtLeast(5f)
                    val stepX = width / (weeklyCounts.size - 1).coerceAtLeast(1)

                    path.moveTo(0f, height)

                    val firstPoint = Offset(0f, height)
                    val points = weeklyCounts.mapIndexed { index, i ->
                        val x = index * stepX
                        val y = height - (i / maxVal) * height * 0.8f
                        Offset(x, y)
                    }

                    path.moveTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]

                        val controlPoint1 = Offset((p0.x + p1.x) / 2, p0.y)
                        val controlPoint2 = Offset((p0.x + p1.x) / 2, p1.y)

                        path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }

                    val strokePath = Path()
                    strokePath.addPath(path)

                    path.lineTo(width, height)
                    path.lineTo(0f, height)
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = Color.White.copy(0.6f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun UserInfoDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update info...") },
        text = {
            Column {
                Text(
                    "Enter information about yourself to help the AI to generate better responses. (e.g. your name, professional, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. I am a software engineer...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

//@Preview(showBackground = true)
//@Composable
//fun MainAppPreview() {
//    ReplyGPTTheme(darkTheme = true) {
//        MainApp(PreferencesManager(LocalContext.current))
//    }
//}