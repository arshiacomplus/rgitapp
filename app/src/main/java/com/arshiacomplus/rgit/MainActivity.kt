package com.arshiacomplus.rgit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arshiacomplus.rgit.data.preferences.ProxyDataStore
import com.arshiacomplus.rgit.ui.screens.MainScreen
import com.arshiacomplus.rgit.ui.theme.RGitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val proxyDataStore = ProxyDataStore(applicationContext)

        setContent {
            RGitTheme {
                MainScreen(proxyDataStore = proxyDataStore)
            }
        }
    }
}