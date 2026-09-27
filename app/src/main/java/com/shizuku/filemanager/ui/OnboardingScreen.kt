package com.shizuku.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to FileManager",
            description = "A powerful file manager for Android. Explore your files like never before.",
            icon = Icons.Default.Folder,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "Engine Options",
            description = "Choose between Standard, SAF, Shizuku, or Root engines to access any part of your storage.",
            icon = Icons.Default.SettingsSuggest,
            color = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            title = "Advanced Tools",
            description = "PDF editing, Media forging, APK signing, and Storage Analysis all in one place.",
            icon = Icons.Default.Construction,
            color = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            title = "Safe & Secure",
            description = "Keep your sensitive files protected in the Safe Folder with encryption and biometric lock.",
            icon = Icons.Default.EnhancedEncryption,
            color = MaterialTheme.colorScheme.secondary
        )
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }

                // Buttons
                TextButton(
                    onClick = onFinished,
                    enabled = true
                ) {
                    Text("SKIP")
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < (pages.size - 1)) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    }
                ) {
                    Text(if (pagerState.currentPage == (pages.size - 1)) "GET STARTED" else "NEXT")
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    tint = page.color
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
