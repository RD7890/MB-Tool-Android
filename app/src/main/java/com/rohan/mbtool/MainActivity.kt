package com.rohan.mbtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohan.mbtool.ui.navigation.AppNavigation
import com.rohan.mbtool.ui.screens.SplashScreen
import com.rohan.mbtool.ui.theme.MBToolTheme
import com.rohan.mbtool.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()

            MBToolTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var splashDone by remember { mutableStateOf(false) }
                    if (splashDone) {
                        AppNavigation()
                    } else {
                        SplashScreen(onFinished = { splashDone = true })
                    }
                }
            }
        }
    }
}
