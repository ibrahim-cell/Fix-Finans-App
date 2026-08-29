package com.fixfinans.app;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ContentValues;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
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

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.gms.common.api.ApiException;
import android.app.PendingIntent;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String APP_URL =
            "https://ibrahim-cell.github.io/Fix-Finans-App/";
    private static final String WEB_CLIENT_ID =
            "399902784452-rl0u63tirl45h5bdgb0hvcog7f343bk6.apps.googleusercontent.com";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int DRIVE_AUTH_REQUEST = 1002;
    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private CredentialManager credentialManager;
    private AuthorizationClient authorizationClient;
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
        authorizationClient = Identity.getAuthorizationClient(this);

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
        webView.addJavascriptInterface(new GoogleDriveBridge(), "AndroidGoogleDrive");
        webView.addJavascriptInterface(new BackupBridge(), "AndroidBackup");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isTrustedAppUrl(url)) {
                    installNativeGoogleBridge();
                } else {
                    view.removeJavascriptInterface("AndroidGoogleSignIn");
                    view.removeJavascriptInterface("AndroidGoogleDrive");
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
                    Intent intent;

                    // WebView'in oluşturduğu intent'i kullan; olmazsa
                    // Android'in sistem belge seçicisine güvenli bir fallback aç.
                    try {
                        intent = params.createIntent();
                    } catch (Exception ignored) {
                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/json");
                    }

                    // Fix Finans yedekleri JSON'dur.
                    // Bazı Android/WebView sürümlerinde createIntent()
                    // filtreyi fazla daraltabildiği için fallback tipi açıkça belirle.
                    if (intent.getType() == null || intent.getType().isEmpty()) {
                        intent.setType("application/json");
                    }

                    intent.addCategory(Intent.CATEGORY_OPENABLE);

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
                "window.__fixNativeDriveAuthSuccess=function(accessToken){" +
                "try{" +
                "if(!window.fixGoogleDrive){window.fixGoogleDrive={};}" +
                "window.fixGoogleDrive.token=accessToken;" +
                "window.fixGoogleDrive.expiresAt=Date.now()+3300000;" +
                "window.fixGoogleDrive.ready=true;" +
                "var p=window.fixGoogleDrive.pendingResolve;" +
                "window.fixGoogleDrive.pendingResolve=null;window.fixGoogleDrive.pendingReject=null;window.fixGoogleDrive.pending=null;" +
                "if(p)p(accessToken);" +
                "}catch(e){if(window.fixGoogleDrive&&window.fixGoogleDrive.pendingReject){var r=window.fixGoogleDrive.pendingReject;window.fixGoogleDrive.pendingResolve=null;window.fixGoogleDrive.pendingReject=null;window.fixGoogleDrive.pending=null;r(e);}}" +
                "};" +
                "window.__fixNativeDriveAuthError=function(message){" +
                "var r=window.fixGoogleDrive&&window.fixGoogleDrive.pendingReject;" +
                "if(window.fixGoogleDrive){window.fixGoogleDrive.pendingResolve=null;window.fixGoogleDrive.pendingReject=null;window.fixGoogleDrive.pending=null;}" +
                "if(r)r(new Error(message||'Google Drive yetkilendirmesi tamamlanamadı.'));" +
                "};" +
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

    private void nativeGoogleDriveAuthorize() {
        if (authorizationClient == null) {
            authorizationClient = Identity.getAuthorizationClient(this);
        }

        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Arrays.asList(new Scope(DRIVE_SCOPE)))
                .build();

        authorizationClient.authorize(request)
                .addOnSuccessListener(this, this::handleDriveAuthorizationResult)
                .addOnFailureListener(this, e -> sendNativeDriveError(
                        e != null && e.getMessage() != null
                                ? e.getMessage()
                                : "Google Drive yetkilendirmesi başlatılamadı."));
    }

    private void handleDriveAuthorizationResult(AuthorizationResult result) {
        if (result == null) {
            sendNativeDriveError("Google Drive yetkilendirme sonucu alınamadı.");
            return;
        }

        if (result.hasResolution() && result.getPendingIntent() != null) {
            try {
                startIntentSenderForResult(
                        result.getPendingIntent().getIntentSender(),
                        DRIVE_AUTH_REQUEST,
                        null,
                        0,
                        0,
                        0);
                return;
            } catch (IntentSender.SendIntentException e) {
                sendNativeDriveError(e.getMessage() != null
                        ? e.getMessage()
                        : "Google Drive izin ekranı açılamadı.");
                return;
            }
        }

        String accessToken = result.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            sendNativeDriveError("Google Drive erişim anahtarı alınamadı.");
            return;
        }

        sendNativeDriveSuccess(accessToken);
    }

    private void sendNativeDriveSuccess(String accessToken) {
        if (webView == null) {
            return;
        }

        String quotedToken = JSONObject.quote(accessToken);
        final String script = "(function(){if(window.__fixNativeDriveAuthSuccess){window.__fixNativeDriveAuthSuccess(" +
                quotedToken + ");}})();";

        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private void sendNativeDriveError(String message) {
        if (webView == null) {
            return;
        }

        String quotedMessage = JSONObject.quote(message == null
                ? "Google Drive yetkilendirmesi tamamlanamadı."
                : message);

        final String script = "(function(){if(window.__fixNativeDriveAuthError){window.__fixNativeDriveAuthError(" +
                quotedMessage + ");}else if(typeof showToast==='function'){showToast(" + quotedMessage + ");}})();";

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

    /**
     * Android WebView does not reliably download Blob/object URLs created by
     * JavaScript. Save JSON backups through MediaStore so they appear in the
     * user's Downloads folder on modern Android versions.
     */
    private final class GoogleDriveBridge {
        @JavascriptInterface
        public void authorize() {
            runOnUiThread(MainActivity.this::nativeGoogleDriveAuthorize);
        }
    }

    private final class BackupBridge {
        @JavascriptInterface
        public void saveJson(String fileName, String json) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "JSON yedeği bu Android sürümünde doğrudan İndirilenler'e kaydedilemiyor.",
                        Toast.LENGTH_LONG).show());
                return;
            }

            new Thread(() -> {
                Uri uri = null;
                try {
                    String safeName = (fileName == null || fileName.trim().isEmpty())
                            ? "fix-finans-yedek.json"
                            : fileName.replaceAll("[\\/:*?\"<>|]", "_");

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Fix Finans");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IllegalStateException("Dosya oluşturulamadı.");

                    try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new IllegalStateException("Dosya yazılamadı.");
                        out.write((json == null ? "" : json).getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }

                    ContentValues done = new ContentValues();
                    done.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, done, null, null);

                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "JSON yedeği İndirilenler / Fix Finans klasörüne kaydedildi.",
                            Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    if (uri != null) {
                        try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
                    }
                    final String message = e.getMessage() == null ? "Dosya kaydedilemedi." : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "JSON yedeği kaydedilemedi: " + message, Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DRIVE_AUTH_REQUEST) {
            if (resultCode != RESULT_OK || data == null) {
                sendNativeDriveError("Google Drive yetkilendirmesi iptal edildi.");
                return;
            }

            try {
                AuthorizationResult result = authorizationClient
                        .getAuthorizationResultFromIntent(data);
                handleDriveAuthorizationResult(result);
            } catch (Exception e) {
                sendNativeDriveError(e.getMessage() != null
                        ? e.getMessage()
                        : "Google Drive yetkilendirmesi tamamlanamadı.");
            }
            return;
        }

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
