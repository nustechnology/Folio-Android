package com.nus.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.presentation.navigation.FolioNavHost
import com.nus.folio.ui.theme.FolioAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as FolioApplication).appContainer

        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                FolioAndroidTheme {
                    FolioNavHost()
                }
            }
        }
    }
}
