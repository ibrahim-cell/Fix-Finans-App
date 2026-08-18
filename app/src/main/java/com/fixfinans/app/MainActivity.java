package com.fixfinans.app;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Launches the live Fix Finans web app in the device browser/Custom Tab.
 * This avoids a WebView so Firebase + Google Authentication stays in a
 * real web authentication context.
 */
public class MainActivity extends Activity {
    private static final String START_URL = "https://ibrahim-cell.github.io/Fix-Finans-App/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openFixFinans();
    }

    private void openFixFinans() {
        Uri uri = Uri.parse(START_URL);
        try {
            CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
            intent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.launchUrl(this, uri);
        } catch (Exception e) {
            android.content.Intent browserIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW, uri);
            try {
                startActivity(browserIntent);
            } catch (Exception ignored) {
                // No compatible browser available.
            }
        }
    }
}
