package com.example.charlobeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.charlobeats.ui.navigation.NavGraph
import com.example.charlobeats.ui.theme.CharloBeatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharloBeatsTheme {
                NavGraph()
            }
        }
    }
}
