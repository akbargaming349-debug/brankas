package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0D1614")),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0D1614"))
    )
    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        BrankasWebScreen()
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrankasWebScreen() {
  var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val cb = filePathCallback
    filePathCallback = null
    if (cb == null) return@rememberLauncherForActivityResult
    val data = result.data
    val uris = mutableListOf<Uri>()
    if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
      val clip = data.clipData
      if (clip != null) {
        for (i in 0 until clip.itemCount) {
          clip.getItemAt(i).uri?.let { uris.add(it) }
        }
      } else {
        data.data?.let { uris.add(it) }
      }
    }
    cb.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
  }

  BackHandler(enabled = webViewRef?.canGoBack() == true) {
    webViewRef?.goBack()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0D1614))
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          setBackgroundColor(android.graphics.Color.parseColor("#0D1614"))
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.databaseEnabled = true
          settings.allowFileAccess = true
          settings.mediaPlaybackRequiresUserGesture = false
          settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
              view: WebView?,
              request: WebResourceRequest?
            ): WebResourceResponse? {
              val url = request?.url ?: return null
              val path = url.path
              if (url.host == "brankas.local" && (path.isNullOrEmpty() || path == "/" || path == "/index.html")) {
                return try {
                  val stream = context.assets.open("index.html")
                  WebResourceResponse(
                    "text/html",
                    "UTF-8",
                    200,
                    "OK",
                    mapOf("Cache-Control" to "no-store, no-cache, must-revalidate"),
                    stream
                  )
                } catch (_: Exception) {
                  null
                }
              }
              return super.shouldInterceptRequest(view, request)
            }
          }
          webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
              webView: WebView?,
              callback: ValueCallback<Array<Uri>>?,
              fileChooserParams: FileChooserParams?
            ): Boolean {
              filePathCallback?.onReceiveValue(null)
              filePathCallback = callback
              val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
              }
              return try {
                filePickerLauncher.launch(Intent.createChooser(intent, "Pilih foto atau video"))
                true
              } catch (_: Exception) {
                filePathCallback = null
                false
              }
            }
          }
          webViewRef = this
          loadUrl("https://brankas.local/index.html")
        }
      }
    )
  }
}
