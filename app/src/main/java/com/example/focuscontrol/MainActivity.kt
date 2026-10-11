package com.example.focuscontrol

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.focuscontrol.ui.components.FocusBottomNavBar
import com.example.focuscontrol.ui.navigation.MainNavDestination
import com.example.focuscontrol.ui.screens.ActiveSessionScreen
import com.example.focuscontrol.ui.screens.AppAllowanceEditorScreen
import com.example.focuscontrol.ui.screens.AppsScreen
import com.example.focuscontrol.ui.screens.BreakSessionScreen
import com.example.focuscontrol.ui.screens.HomeScreen
import com.example.focuscontrol.ui.screens.ScheduleEditorScreen
import com.example.focuscontrol.ui.screens.ScheduleScreen
import com.example.focuscontrol.ui.screens.SettingsScreen
import com.example.focuscontrol.ui.screens.StatsScreen
import com.example.focuscontrol.model.AppInfo
import com.example.focuscontrol.model.StudySchedule
import com.example.focuscontrol.ui.theme.FocusControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.focuscontrol.ui.theme.NavyBackground
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.focuscontrol.model.PlannerTask

import com.example.focuscontrol.ui.screens.MyStudyMaterialsScreen
import com.example.focuscontrol.ui.screens.PlannerHomeScreen
import com.example.focuscontrol.ui.screens.TaskEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusControlTheme {
                FocusControlAppMainContent()
            }
        }
    }
}

@Composable
fun FocusControlAppMainContent() {
    val context = LocalContext.current
    val prefs = remember { FocusPreferences(context) }
    val plannerPrefs = remember { PlannerPreferences(context) }

    var currentDestination by rememberSaveable { mutableStateOf(MainNavDestination.HOME) }
    var studyModeEnabled by remember { mutableStateOf(prefs.isStudyModeEnabled) }
    var isServiceEnabled by remember { mutableStateOf(FocusBlockerService.isServiceEnabled(context)) }
    var blockedPackages by remember { mutableStateOf(prefs.blockedPackageNames) }
    var isOnBreak by remember { mutableStateOf(prefs.isStudyModeOnBreak()) }
    var studySchedules by remember { mutableStateOf(prefs.studySchedules) }

    var showScheduleEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<StudySchedule?>(null) }
    var showAllowanceEditor by remember { mutableStateOf(false) }
    var editingAllowancePackage by remember { mutableStateOf("") }
    var showActiveSession by remember { mutableStateOf(false) }
    var showBreakSession by remember { mutableStateOf(false) }
    

    var showStudyMaterials by remember { mutableStateOf(false) }
    var showScheduleList by remember { mutableStateOf(false) }
    
    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<PlannerTask?>(null) }
    var selectedTaskDate by remember { mutableStateOf("") }

    // Back button double-tap-to-exit state
    var lastBackPressTimeMs by remember { mutableStateOf(0L) }
    
    // Refresh trigger for planner data
    var plannerRefreshTrigger by remember { mutableStateOf(0) }

    BackHandler(enabled = true) {
        when {
            showTaskEditor -> {
                showTaskEditor = false
                editingTask = null
            }
            showStudyMaterials -> showStudyMaterials = false
            showScheduleEditor -> {
                showScheduleEditor = false
                editingSchedule = null
            }
            showScheduleList -> showScheduleList = false
            showAllowanceEditor -> {
                showAllowanceEditor = false
            }
            currentDestination != MainNavDestination.HOME -> {
                currentDestination = MainNavDestination.HOME
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTimeMs < 2000L) {
                    (context as? ComponentActivity)?.finish()
                } else {
                    lastBackPressTimeMs = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Cached state for installed apps
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val selfPkg = context.packageName
            val apps = resolveInfos.mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == selfPkg) null
                else {
                    val label = resolve.loadLabel(pm).toString()
                    val iconDrawable = try { resolve.loadIcon(pm) } catch (_: Exception) { null }
                    val iconBitmap = try { iconDrawable?.toBitmap()?.asImageBitmap() } catch (_: Exception) { null }
                    AppInfo(pkg, label, iconBitmap)
                }
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }

            installedApps = apps
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = FocusBlockerService.isServiceEnabled(context)
                blockedPackages = prefs.blockedPackageNames
                
                // Reconcile current session state with the saved schedule on app reopen
                val now = System.currentTimeMillis()
                val scheduleInfo = ScheduleReceiver.evaluateActiveScheduleInfo(context, prefs.studySchedules, now)
                
                if (scheduleInfo.isStudyActive) {
                    prefs.isStudyModeEnabled = true
                    if (scheduleInfo.isBreakActive) {
                        val existingBreakEnd = prefs.studyModeBreakEndTimeMs
                        if (existingBreakEnd < now) {
                            prefs.studyModeBreakEndTimeMs = now + 60 * 1000L
                        }
                    } else {
                        prefs.endStudyModeBreak()
                    }
                } else if (!prefs.isStudyExtensionActive()) {
                    prefs.isStudyModeEnabled = false
                    prefs.endStudyModeBreak()
                }
                
                studyModeEnabled = prefs.isStudyModeEnabled
                isOnBreak = prefs.isStudyModeOnBreak()
                ScheduleManager.rescheduleAll(context, prefs.studySchedules)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NavyBackground
    ) {
        // Handle Full-Screen Modal Overlay Screens
        when {
            showTaskEditor -> {
                TaskEditorScreen(
                    initialDateString = selectedTaskDate,
                    existingTask = editingTask,
                    plannerPrefs = plannerPrefs,
                    onSave = { updatedTask ->
                        plannerPrefs.saveTask(updatedTask)
                        plannerRefreshTrigger++
                        showTaskEditor = false
                        editingTask = null
                    },
                    onDelete = { taskId ->
                        plannerPrefs.deleteTask(taskId)
                        plannerRefreshTrigger++
                        showTaskEditor = false
                        editingTask = null
                    },
                    onCancel = {
                        plannerRefreshTrigger++
                        showTaskEditor = false
                        editingTask = null
                    }
                )
            }

            showStudyMaterials -> {
                MyStudyMaterialsScreen(
                    onBack = { showStudyMaterials = false },
                    onOpenTask = { taskId ->
                        showStudyMaterials = false
                        editingTask = plannerPrefs.tasks.find { it.id == taskId }
                        if (editingTask != null) {
                            showTaskEditor = true
                        }
                    }
                )
            }
            showScheduleList -> {
                ScheduleScreen(
                    schedules = studySchedules,
                    onAddSchedule = {
                        editingSchedule = null
                        showScheduleEditor = true
                    },
                    onEditSchedule = { schedule ->
                        editingSchedule = schedule
                        showScheduleEditor = true
                    },
                    onToggleSchedule = { schedule, enabled ->
                        val updated = schedule.copy(isEnabled = enabled)
                        prefs.saveStudySchedule(updated)
                        studySchedules = prefs.studySchedules
                        ScheduleManager.rescheduleAll(context, studySchedules)
                    },
                    onBack = { showScheduleList = false }
                )
            }
            showScheduleEditor -> {
                ScheduleEditorScreen(
                    initialSchedule = editingSchedule,
                    onSave = { updatedSchedule ->
                        prefs.saveStudySchedule(updatedSchedule)
                        studySchedules = prefs.studySchedules
                        ScheduleManager.rescheduleAll(context, studySchedules)
                        showScheduleEditor = false
                        editingSchedule = null
                    },
                    onDelete = { scheduleToDelete ->
                        prefs.deleteStudySchedule(scheduleToDelete.id)
                        studySchedules = prefs.studySchedules
                        ScheduleManager.rescheduleAll(context, studySchedules)
                        showScheduleEditor = false
                        editingSchedule = null
                    },
                    onCancel = {
                        showScheduleEditor = false
                        editingSchedule = null
                    },
                )
            }
            showAllowanceEditor -> {
                AppAllowanceEditorScreen(
                    packageName = editingAllowancePackage,
                    existingRule = prefs.getAllowanceRule(editingAllowancePackage),
                    onSaveRule = { rule ->
                        prefs.saveAllowanceRule(rule)
                        showAllowanceEditor = false
                    },
                    onDeleteRule = { pkg ->
                        prefs.deleteAllowanceRule(pkg)
                        showAllowanceEditor = false
                    },
                    onCancel = {
                        showAllowanceEditor = false
                    },
                )
            }
            else -> {
                val saveableStateHolder = rememberSaveableStateHolder()
                
                // Main Scaffold with Persistent Bottom Navigation Bar
                Scaffold(
                    bottomBar = {
                        FocusBottomNavBar(
                            currentDestination = currentDestination,
                            onNavigateTo = { destination ->
                                currentDestination = destination
                            }
                        )
                    },
                    containerColor = NavyBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        saveableStateHolder.SaveableStateProvider(key = currentDestination) {
                            when (currentDestination) {
                                MainNavDestination.HOME -> {
                                    PlannerHomeScreen(
                                        plannerPrefs = plannerPrefs,
                                        refreshTrigger = plannerRefreshTrigger,
                                        onNavigateToStudyMaterials = { showStudyMaterials = true },
                                        onOpenTask = { taskId -> 
                                            editingTask = plannerPrefs.tasks.find { it.id == taskId }
                                            if (editingTask != null) {
                                                showTaskEditor = true
                                            }
                                        },
                                        onAddTask = { dateStr -> 
                                            selectedTaskDate = dateStr
                                            editingTask = null
                                            showTaskEditor = true
                                        },
                                        onToggleTaskCompletion = { updatedTask ->
                                            plannerPrefs.saveTask(updatedTask)
                                            plannerRefreshTrigger++
                                        }
                                    )
                                }
                                MainNavDestination.STUDY -> {
                                    HomeScreen(
                                        context = context,
                                        isServiceEnabled = isServiceEnabled,
                                        studyModeEnabled = studyModeEnabled,
                                        isOnBreak = isOnBreak,
                                        blockedPackages = blockedPackages,
                                        installedApps = installedApps,
                                        studySchedules = studySchedules,
                                        onEndBreak = {
                                            prefs.endStudyModeBreak()
                                            isOnBreak = false
                                        },
                                        onToggleBlockedApp = { pkg, isBlocked ->
                                            prefs.setAppBlocked(pkg, isBlocked)
                                            blockedPackages = prefs.blockedPackageNames
                                        },
                                        onNavigateToSchedule = {
                                            showScheduleList = true
                                        },
                                        onNavigateToStats = {
                                            currentDestination = MainNavDestination.STATS
                                        },
                                        onNavigateToSettings = {
                                            currentDestination = MainNavDestination.SETTINGS
                                        }
                                    )
                                }
                                MainNavDestination.APP_CONTROL -> {
                                    AppsScreen(
                                        blockedPackages = blockedPackages,
                                        installedApps = installedApps,
                                        onToggleBlockedApp = { pkg, isBlocked ->
                                            prefs.setAppBlocked(pkg, isBlocked)
                                            blockedPackages = prefs.blockedPackageNames
                                        },
                                        onConfigureAllowanceRule = { pkg ->
                                            editingAllowancePackage = pkg
                                            showAllowanceEditor = true
                                        }
                                    )
                                }
                                MainNavDestination.STATS -> {
                                    StatsScreen()
                                }
                                MainNavDestination.SETTINGS -> {
                                    SettingsScreen(
                                        context = context,
                                        isServiceEnabled = isServiceEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
