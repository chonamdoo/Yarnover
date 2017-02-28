package co.kenrg.yarnover.oauth

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import co.kenrg.yarnover.oauth.OAuthManager.AUTH_CALLBACK_URL
import org.jetbrains.anko.webView

class OAuthActivity : AppCompatActivity() {
  private lateinit var context: Context

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    context = this

    val authorizationUri = intent.data

    webView {
      setWebChromeClient(WebChromeClient())
      setWebViewClient(object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
          super.onPageStarted(view, url, favicon)
          if (url.startsWith(AUTH_CALLBACK_URL)) {
            view.stopLoading()
            val intent = Intent(context, AuthenticateActivity::class.java)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
          }
        }
      })

      loadUrl(authorizationUri.toString())
    }
  }
}