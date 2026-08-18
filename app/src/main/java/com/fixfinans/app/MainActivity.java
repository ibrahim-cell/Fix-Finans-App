package com.fixfinans.app;

import android.os.Bundle;
import com.google.androidbrowserhelper.trusted.LauncherActivity;

/**
 * Fix Finans Android shell.
 * Launches the live GitHub Pages PWA in a Trusted Web Activity / Custom Tab fallback.
 * This keeps Firebase + Google Authentication in the real browser context.
 */
public class MainActivity extends LauncherActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
