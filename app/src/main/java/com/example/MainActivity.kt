package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.JarvisApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize JarvisViewModel safely using ViewModelProvider
        val viewModel = ViewModelProvider(this)[JarvisViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(
                themeMode = viewModel.themeMode,
                accentColorIndex = viewModel.accentColorIndex
            ) {
                JarvisApp(viewModel = viewModel)
            }
        }
    }
}
