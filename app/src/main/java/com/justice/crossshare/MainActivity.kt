package com.justice.crossshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.justice.crossshare.ui.MainScreen
import com.justice.crossshare.ui.theme.CrossShareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossShareTheme {
                MainScreen()
            }
        }
    }
}
