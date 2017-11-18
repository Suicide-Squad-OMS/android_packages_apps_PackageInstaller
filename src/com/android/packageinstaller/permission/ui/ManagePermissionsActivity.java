/*
* Copyright (C) 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.packageinstaller.permission.ui;

import android.app.Fragment;
import android.app.IThemeCallback;
import android.app.ThemeManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.packageinstaller.permission.ui.wear.AppPermissionsFragmentWear;
import com.android.packageinstaller.R;
import com.android.packageinstaller.DeviceUtils;

public final class ManagePermissionsActivity extends OverlayTouchActivity {
    private static final String LOG_TAG = "ManagePermissionsActivity";

    private int mTheme;
    private ThemeManager mThemeManager;

    private final IThemeCallback mThemeCallback = new IThemeCallback.Stub() {

        @Override
        public void onThemeChanged(int themeMode, int color) {
            onCallbackAdded(themeMode, color);
            ManagePermissionsActivity.this.runOnUiThread(() -> {
                ManagePermissionsActivity.this.recreate();
            });
        }

        @Override
        public void onCallbackAdded(int themeMode, int color) {
            mTheme = color;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final int themeMode = Secure.getInt(getContentResolver(),
                Secure.THEME_PRIMARY_COLOR, 0);
        final int accentColor = Secure.getInt(getContentResolver(),
                Secure.THEME_ACCENT_COLOR, 0);
        mThemeManager = (ThemeManager) getSystemService(Context.THEME_SERVICE);
        if (mThemeManager != null) {
            mThemeManager.addCallback(mThemeCallback);
        }
        if (themeMode != 0 || accentColor != 0) {
            getTheme().applyStyle(mTheme, true);
        }
        if (themeMode == 2) {
            getTheme().applyStyle(R.style.settings_pixel_theme, true);
        }

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            return;
        }

        Fragment fragment;
        String action = getIntent().getAction();

        switch (action) {
            case Intent.ACTION_MANAGE_PERMISSIONS: {
                if (DeviceUtils.isTelevision(this)) {
                    fragment = com.android.packageinstaller.permission.ui.television
                            .ManagePermissionsFragment.newInstance();
                } else {
                    fragment = com.android.packageinstaller.permission.ui.handheld
                            .ManagePermissionsFragment.newInstance();
                }
            } break;

            case Intent.ACTION_MANAGE_APP_PERMISSIONS: {
                String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
                if (packageName == null) {
                    Log.i(LOG_TAG, "Missing mandatory argument EXTRA_PACKAGE_NAME");
                    finish();
                    return;
                }
                if (DeviceUtils.isWear(this)) {
                    fragment = AppPermissionsFragmentWear.newInstance(packageName);
                } else if (DeviceUtils.isTelevision(this)) {
                    fragment = com.android.packageinstaller.permission.ui.television
                            .AppPermissionsFragment.newInstance(packageName);
                } else {
                    fragment = com.android.packageinstaller.permission.ui.handheld
                            .AppPermissionsFragment.newInstance(packageName);
                }
            } break;

            case Intent.ACTION_MANAGE_PERMISSION_APPS: {
                String permissionName = getIntent().getStringExtra(Intent.EXTRA_PERMISSION_NAME);
                if (permissionName == null) {
                    Log.i(LOG_TAG, "Missing mandatory argument EXTRA_PERMISSION_NAME");
                    finish();
                    return;
                }
                if (DeviceUtils.isTelevision(this)) {
                    fragment = com.android.packageinstaller.permission.ui.television
                            .PermissionAppsFragment.newInstance(permissionName);
                } else {
                    fragment = com.android.packageinstaller.permission.ui.handheld
                            .PermissionAppsFragment.newInstance(permissionName);
                }
            } break;

            default: {
                Log.w(LOG_TAG, "Unrecognized action " + action);
                finish();
                return;
            }
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
}
