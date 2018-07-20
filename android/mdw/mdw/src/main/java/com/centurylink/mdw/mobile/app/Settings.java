/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.mobile.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.centurylink.mdw.mobile.R;
import com.centurylink.mdw.mobile.model.App;
import com.centurylink.mdw.mobile.model.NavDrawerItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings {
    private static final String TAG = Settings.class.getSimpleName();

    public static final String CENTRAL_URL = "central_url";
    public static final String DEFAULT_CENTRAL_URL = "https://mdw-central.com";
    public static final String HELP_DOCS_URL = "https://centurylinkcloud.github.io/mdw/docs/help/";
    public static final String MDW_SITE_URL = "https://centurylinkcloud.github.io/mdw/";

    private static final String USERNAME = "username";
    private static final String APPS = "apps";
    private static final String APP_TOKENS = "app_tokens";
    private static final String ENV = "env";

    private static final String CURRENT_NAV_DRAWER = "currentNavDrawer";
    private static final String CURRENT_TAB = "currentTab";

    public static final int DEFAULT_CONNECT_TIMEOUT = 15;
    public static final int DEFAULT_READ_TIMEOUT = 15;

    private Context appContext;
    public Context getAppContext() { return appContext; }

    private SharedPreferences prefs;
    public SharedPreferences getPrefs() { return prefs; }

    public Settings(Context appContext) {
        this.appContext = appContext;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public URL getCentralUrl() {
        String pref = prefs.getString(CENTRAL_URL, null);
        if (pref == null)
            pref = DEFAULT_CENTRAL_URL;
        try {
            return new URL(pref);
        }
        catch (MalformedURLException ex) {
            throw new BadSettingsException("Invalid Central URL: " + pref);
        }
    }

    public List<App> getApps() {
        String appsStr = prefs.getString(APPS, null);
        if (appsStr == null || appsStr.isEmpty()) {
            return new ArrayList<>();
        } else {
            try {
                return App.parseApps(new JSONObject(appsStr));
            } catch (JSONException | MalformedURLException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                throw new BadSettingsException("Invalid Apps JSON");
            }
        }
    }

    public void setApps(List<App> apps) {
        try {
            prefs.edit().putString(APPS, apps == null ? null : App.stringifyApps(apps)).commit();
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new BadSettingsException("Invalid Apps JSON");
        }
    }

    public App.Env getEnv() {
        String env = prefs.getString(ENV, null);
        if (env != null) {
            try {
                JSONObject envObj = new JSONObject(env);
                List<App> apps = getApps();
                if (apps != null) {
                    for (App app : apps) {
                        if (app.getId().equals(envObj.getString("appId"))) {
                            if (app.getEnvs() != null) {
                                for (App.Env appEnv : app.getEnvs()) {
                                    if (appEnv.getName().equals(envObj.getString("name")))
                                        return appEnv;
                                }
                            }
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                throw new BadSettingsException("Invalid Env JSON");
            }
        }
        return null;
    }

    public void setEnv(App.Env env) throws BadSettingsException {
        setNavDrawerItems(null);
        setCurrentNavDrawer(null);
        setCurrentTab(null);
        try {
            String envStr = null;
            if (env != null) {
                JSONObject json = new JSONObject();
                json.put("appId", env.getApp().getId());
                json.put("name", env.getName());
                envStr = json.toString();
            }
            prefs.edit().putString(ENV, envStr).commit();
        }
        catch (JSONException ex) {
            throw new BadSettingsException("Invalid Env JSON");
        }
    }

    public String getUsername() {
        String user = prefs.getString(USERNAME, null);
        return user == null || user.isEmpty() ? null : user;
    }

    public void setUsername(String user) {
        prefs.edit().putString(USERNAME, user).commit();
    }

    public String getToken(String appId) {
        String tokens = prefs.getString(APP_TOKENS, null);
        if (tokens == null)
            return null;
        try {
            JSONObject json = new JSONObject(tokens);
            if (json.has(appId))
                return json.getString(appId);
            else
                return null;
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new BadSettingsException("Invalid App Tokens JSON");
        }
    }

    public void clearTokens() {
        prefs.edit().putString(APP_TOKENS, null).commit();
    }

    public void setToken(String appId, String token) {
        String tokens = prefs.getString(APP_TOKENS, null);
        if (tokens == null)
            tokens = "{}";
        try {
            JSONObject json = new JSONObject(tokens);
            if (token == null)
                json.remove(appId);
            else
                json.put(appId, token);
            prefs.edit().putString(APP_TOKENS, json.toString()).commit();
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new BadSettingsException("Invalid App Tokens JSON");
        }
    }

    public String getCurrentNavDrawer() {
        String cur = prefs.getString(CURRENT_NAV_DRAWER, null);
        return cur == null || cur.isEmpty() ? null : cur;
    }

    /**
     * Sets in background using apply().
     */
    public void setCurrentNavDrawer(String navDrawer) {
        prefs.edit().putString(CURRENT_NAV_DRAWER, navDrawer).apply();
    }

    public String getCurrentTab() {
        String cur = prefs.getString(CURRENT_TAB, null);
        return cur == null || cur.isEmpty() ? null : cur;
    }

    /**
     * Sets in background using apply().
     */
    public void setCurrentTab(String tab) {
        prefs.edit().putString(CURRENT_TAB, tab).apply();
    }

    private Map<String,NavDrawerItem> navDrawerItems;
    public Map<String,NavDrawerItem> getNavDrawerItems() { return navDrawerItems; }
    public void setNavDrawerItems(Map<String,NavDrawerItem> items) {
        navDrawerItems = items;
    }

    public Map<String,String> getRequestHeaders(String appId) {
        Map<String,String> headers = new HashMap<>();
        if (appId != null) {
            String token = getToken(appId);
            if (token != null)
                headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public void logout() {
        clearTokens();
        setUsername(null);
        setApps(null);
        setEnv(null);
    }

    public boolean isErrorReportingEnabled() {
        return true;
    }

    public boolean isTablet() {
        return appContext.getResources().getBoolean(R.bool.isTablet);
    }

    public boolean isLandscape() {
        return appContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    public String getMdwMobileVersion() {
        PackageManager manager = appContext.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(appContext.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException ex) { // should never happen
            Log.e(TAG, ex.getMessage(), ex);
            return ex.toString();
        }
    }

    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }
}
