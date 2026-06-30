package com.dragonpi.timegem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonpi.timegem.ui.theme.TimeGemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeGemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 1. Called the welcome layout here so it shows on a phone
                    TimeGemWelcomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TimeGemWelcomeScreen(modifier: Modifier = Modifier) {
    // 2. Used a Column to center Giusto and your text perfectly on screen
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimeGemLogoHeader()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Time Gem",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Get your life 'giusto'!",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TimeGemLogoHeader() {
    Image(
        painter = painterResource(id = R.drawable.ic_time_gem_logo),
        contentDescription = "Time Gem Official Logo featuring Giusto",
        modifier = Modifier.size(140.dp) // Bumped up slightly to showcase the detail
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    TimeGemTheme {
        // 3. Updated the preview to show the actual design layout
        TimeGemWelcomeScreen()
    }
}