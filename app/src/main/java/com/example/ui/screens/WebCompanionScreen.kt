package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryBlue
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebCompanionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Web Companion",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (isDesktopMode) "DESKTOP" else "LIVE WEB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Strat Analytics • Interactive Web Platform",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9EA8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("web_companion_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Viewport Toggle (Desktop vs Mobile simulation)
                    IconButton(
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewRef?.let { wv ->
                                val defaultUa = wv.settings.userAgentString
                                if (isDesktopMode) {
                                    wv.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                    wv.setInitialScale(80)
                                } else {
                                    wv.settings.userAgentString = null
                                    wv.setInitialScale(100)
                                }
                                wv.reload()
                            }
                        },
                        modifier = Modifier.testTag("toggle_desktop_mode")
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Filled.Laptop else Icons.Outlined.Smartphone,
                            contentDescription = "Toggle Desktop/Mobile Viewport",
                            tint = if (isDesktopMode) Color(0xFF34D399) else Color(0xFFCBD5E1)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            isLoading = true
                            webViewRef?.reload()
                        },
                        modifier = Modifier.testTag("web_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Web App",
                            tint = Color.White
                        )
                    }

                    // Share / Export HTML File
                    IconButton(
                        onClick = {
                            exportWebBundle(context)
                        },
                        modifier = Modifier.testTag("web_export_html_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Export Website HTML",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F17))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl("file:///android_asset/web/index.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("web_companion_webview")
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0F17)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Loading Strat Analytics Web Platform...",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Copies the self-contained assets/web/index.html into cache directory and triggers Android Share Sheet.
 */
private fun exportWebBundle(context: Context) {
    try {
        val assetManager = context.assets
        val inputStream = assetManager.open("web/index.html")
        val exportFile = File(context.cacheDir, "strat_analytics_web_companion.html")
        FileOutputStream(exportFile).use { output ->
            inputStream.copyTo(output)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Strat Analytics Web Companion Dashboard")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is the standalone Strat Analytics web application file. Open in any desktop or mobile browser to view your venture studio dashboard!"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Standalone Website HTML"))
    } catch (e: Exception) {
        // Fallback: toast that file is ready
        Toast.makeText(context, "Exported HTML to cache/strat_analytics_web_companion.html", Toast.LENGTH_LONG).show()
    }
}
