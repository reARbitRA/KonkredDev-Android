package com.example.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val PREVIEW_HOST = "localproject"

@Composable
fun HtmlPreview(
    htmlCode: String,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Destroy the WebView when the preview leaves composition (prevents native leaks)
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
            webViewRef = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Configure hardened sandboxed WebView for untrusted (user/AI-generated) HTML
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                // Never allow file:// or content:// access from previewed documents
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false

                webViewClient = object : WebViewClient() {
                    // Lock navigation to the virtual preview origin; block all external URLs
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val host = request?.url?.host
                        return host != PREVIEW_HOST
                    }
                }
                webChromeClient = WebChromeClient()
                webViewRef = this
            }
        },
        update = { webView ->
            // Reload with updated coding buffer contents
            webView.loadDataWithBaseURL(
                "http://$PREVIEW_HOST/", // Virtual directory url helper
                htmlCode,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier.fillMaxSize()
    )
}
