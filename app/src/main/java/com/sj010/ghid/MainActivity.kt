package com.sj010.ghid

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sj010.ghid.domain.model.ThemeMode
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.combos.ComboStudioScreen
import com.sj010.ghid.ui.dashboard.DashboardScreen
import com.sj010.ghid.ui.interactive.InteractiveModeScreen
import com.sj010.ghid.ui.navigation.Screen
import com.sj010.ghid.ui.settings.SettingsScreen
import com.sj010.ghid.ui.streamdeck.StreamDeckScreen
import com.sj010.ghid.ui.theme.GhostHidTheme
import com.sj010.ghid.ui.trackpad.TrackpadScreen

class MainActivity : ComponentActivity() {

    private var hidService by mutableStateOf<HidService?>(null)
    private var isBound by mutableStateOf(false)
    private var isQuitting = false

    /** Bumped when the notification content intent is tapped; the scaffold jumps home. */
    private var openDashboardRequests by mutableStateOf(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? HidService.LocalBinder
            hidService = binder?.service
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (hasRequiredBluetoothPermission()) {
            startAndBindService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Material You Expressive splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        preferHighRefreshRate()

        // "Stop gateway" from the notification: quit through the regular path so the
        // service is torn down *and* the app is closed, never left half-stopped.
        if (intent?.getBooleanExtra(HidService.EXTRA_QUIT_APP, false) == true) {
            quitApp()
            return
        }

        if (hasRequiredBluetoothPermission()) {
            startAndBindService()
        } else {
            requestRequiredPermissions()
        }

        setContent {
            val settings by GhostHidApp.instance.settingsRepository.settings.collectAsState()

            // Keep screen awake toggle
            DisposableEffect(settings.keepScreenAwake) {
                if (settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // High refresh rate toggle
            LaunchedEffect(settings.highRefreshRate) {
                if (settings.highRefreshRate) {
                    preferHighRefreshRate()
                } else {
                    window.attributes = window.attributes.apply {
                        preferredRefreshRate = 60f
                    }
                }
            }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            GhostHidTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                MainAppScaffold(
                    service = hidService,
                    onQuit = { quitApp() },
                    dashboardRequests = openDashboardRequests
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(HidService.EXTRA_QUIT_APP, false)) {
            // Quitting clears the task, so there is nothing left to navigate.
            quitApp()
            return
        }
        if (intent.getBooleanExtra(HidService.EXTRA_OPEN_DASHBOARD, false)) {
            openDashboardRequests++
        }
    }

    private fun quitApp() {
        if (isQuitting) return
        isQuitting = true

        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(1001)
            nm?.cancelAll()
        } catch (ignored: Exception) {}

        try {
            hidService?.quit()
        } catch (ignored: Exception) {}

        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (ignored: Exception) {}
            isBound = false
            hidService = null
        }

        try {
            stopService(Intent(this, HidService::class.java))
        } catch (ignored: Exception) {}

        finishAffinity()
        finishAndRemoveTask()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(1001)
                nm?.cancelAll()
            } catch (ignored: Exception) {}
            Process.killProcess(Process.myPid())
            kotlin.system.exitProcess(0)
        }, 400)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isQuitting) {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(1001)
                nm?.cancelAll()
            } catch (ignored: Exception) {}
        }
    }

    private fun preferHighRefreshRate() {
        val displaySupports120Hz = display?.supportedModes?.any { it.refreshRate >= 119.5f } == true
        if (displaySupports120Hz) {
            window.attributes = window.attributes.apply {
                preferredRefreshRate = 120f
            }
        }
    }

    private fun hasRequiredBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startAndBindService() {
        if (!hasRequiredBluetoothPermission()) return
        val intent = Intent(this, HidService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            // Background start restrictions or security exception
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun MainAppScaffold(service: HidService?, onQuit: () -> Unit = {}, dashboardRequests: Int = 0) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    // A notification tap always lands on the dashboard, even if another tab is showing.
    LaunchedEffect(dashboardRequests) {
        if (dashboardRequests > 0) currentScreen = Screen.Dashboard
    }

    BackHandler(enabled = currentScreen == Screen.Settings || currentScreen == Screen.Interactive) {
        currentScreen = Screen.Dashboard
    }

    // Landscape turns the phone into a full-bleed stream deck. Portrait keeps
    // the regular bottom-nav scaffold, and the current screen survives rotation.
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        StreamDeckScreen(service = service)
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Interactive mode hands the bottom of the screen to the system IME.
            if (currentScreen != Screen.Interactive) {
                NavigationBar(
                    modifier = Modifier.alpha(if (currentScreen == Screen.Settings) 0f else 1f),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Screen.bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            enabled = currentScreen != Screen.Settings,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1,
                                    fontSize = 11.sp,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)).togetherWith(
                        fadeOut(animationSpec = tween(180))
                    )
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        DashboardScreen(
                            service = service,
                            onNavigateToCombos = { currentScreen = Screen.Combos },
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            onNavigateToInteractive = { currentScreen = Screen.Interactive },
                            onQuit = onQuit
                        )
                    }
                    Screen.Combos -> ComboStudioScreen(service = service)
                    Screen.Trackpad -> TrackpadScreen(service = service)
                    Screen.Interactive -> InteractiveModeScreen(
                        service = service,
                        onExit = { currentScreen = Screen.Dashboard }
                    )
                    Screen.Settings -> SettingsScreen(onBack = { currentScreen = Screen.Dashboard })
                }
            }
        }
    }
}
