package com.fixfinans.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.CancellationSignal;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String APP_URL =
            "https://ibrahim-cell.github.io/Fix-Finans-App/";
    private static final String WEB_CLIENT_ID =
            "399902784452-rl0u63tirl45h5bdgb0hvcog7f343bk6.apps.googleusercontent.com";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private CredentialManager credentialManager;
    private final Executor credentialExecutor = Executors.newSingleThreadExecutor();
    private boolean googleSignInInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(5, 6, 13));
        getWindow().setNavigationBarColor(Color.rgb(5, 6, 13));

        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        webView = new WebView(this);
        credentialManager = CredentialManager.create(this);

        webView.setBackgroundColor(Color.rgb(5, 6, 13));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= 21) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(new GoogleAuthBridge(), "AndroidGoogleSignIn");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isTrustedAppUrl(url)) {
                    installNativeGoogleBridge();
                } else {
                    view.removeJavascriptInterface("AndroidGoogleSignIn");
                }
            }
        });

        // Android WebView'da HTML <input type="file"> için sistem dosya seçiciyi aç.
        // Bu özellikle Misafir Modu > Yedekleme Merkezi > Geri Yükle akışı için gereklidir.
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(APP_URL);
    }

    private boolean isTrustedAppUrl(String url) {
        return url != null && (url.equals(APP_URL) || url.startsWith(APP_URL));
    }

    private void installNativeGoogleBridge() {
        if (webView == null) {
            return;
        }

        String script = "(function(){try{" +
                "window.__fixNativeGoogleAuthSuccess=function(idToken){" +
                "try{" +
                "if(!window.firebase||!firebase.auth){" +
                "if(typeof showToast==='function')showToast('Google oturumu hazır değil.');return;" +
                "}" +
                "var credential=firebase.auth.GoogleAuthProvider.credential(idToken);" +
                "firebase.auth().signInWithCredential(credential).then(function(){" +
                "if(typeof setGoogleButtonBusy==='function')setGoogleButtonBusy(false);" +
                "if(typeof syncWelcomeGate==='function')syncWelcomeGate();" +
                "if(typeof render==='function')render();" +
                "}).catch(function(e){" +
                "if(typeof setGoogleButtonBusy==='function')setGoogleButtonBusy(false);" +
                "if(typeof showToast==='function')showToast('Google bağlantısı kurulamadı: '+(e&&e.message?e.message:'bilinmeyen hata'));" +
                "});" +
                "}catch(e){" +
                "if(typeof setGoogleButtonBusy==='function')setGoogleButtonBusy(false);" +
                "if(typeof showToast==='function')showToast('Google bağlantısı kurulamadı.');" +
                "}};" +
                "window.__fixNativeGoogleAuthError=function(message){" +
                "if(typeof setGoogleButtonBusy==='function')setGoogleButtonBusy(false);" +
                "if(typeof showToast==='function')showToast(message||'Google giriş işlemi tamamlanamadı.');" +
                "};" +
                "if(window.AndroidGoogleSignIn){" +
                "window.signInWithGoogle=function(){" +
                "if(typeof setGoogleButtonBusy==='function')setGoogleButtonBusy(true);" +
                "window.AndroidGoogleSignIn.signIn();" +
                "};" +
                "}" +
                "}catch(e){}})();";

        webView.evaluateJavascript(script, null);
    }

    private void nativeGoogleSignIn() {
        if (googleSignInInProgress) {
            return;
        }

        googleSignInInProgress = true;

        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .setNonce(generateSecureNonce())
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleGoogleCredential(response);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        googleSignInInProgress = false;
                        String message = e != null && e.getMessage() != null
                                ? e.getMessage()
                                : "Google giriş işlemi tamamlanamadı.";
                        sendNativeGoogleError(message);
                    }
                }
        );
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        try {
            Credential credential = response.getCredential();

            if (!(credential instanceof CustomCredential)) {
                throw new IllegalStateException("Beklenmeyen Google kimlik bilgisi türü.");
            }

            CustomCredential customCredential = (CustomCredential) credential;

            if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {
                throw new IllegalStateException("Beklenmeyen Google kimlik bilgisi türü.");
            }

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(customCredential.getData());

            String idToken = googleCredential.getIdToken();

            if (idToken == null || idToken.isEmpty()) {
                throw new IllegalStateException("Google ID token alınamadı.");
            }

            sendNativeGoogleSuccess(idToken);
        } catch (Exception e) {
            googleSignInInProgress = false;
            sendNativeGoogleError(e.getMessage() != null
                    ? e.getMessage()
                    : "Google giriş işlemi tamamlanamadı.");
        }
    }

    private void sendNativeGoogleSuccess(String idToken) {
        googleSignInInProgress = false;

        if (webView == null) {
            return;
        }

        String quotedToken = JSONObject.quote(idToken);
        final String script = "(function(){if(window.__fixNativeGoogleAuthSuccess){window.__fixNativeGoogleAuthSuccess(" +
                quotedToken + ");}else if(typeof showToast==='function'){showToast('Google bağlantısı hazırlanıyor...');}})();";

        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private void sendNativeGoogleError(String message) {
        if (webView == null) {
            return;
        }

        String quotedMessage = JSONObject.quote(message == null
                ? "Google giriş işlemi tamamlanamadı."
                : message);

        final String script = "(function(){if(window.__fixNativeGoogleAuthError){window.__fixNativeGoogleAuthError(" +
                quotedMessage + ");}})();";

        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private String generateSecureNonce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        if (Build.VERSION.SDK_INT >= 26) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
    }

    private final class GoogleAuthBridge {
        @JavascriptInterface
        public void signIn() {
            runOnUiThread(MainActivity.this::nativeGoogleSignIn);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.destroy();
            webView = null;
        }

        if (credentialExecutor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) credentialExecutor).shutdownNow();
        }

        super.onDestroy();
    }
}
