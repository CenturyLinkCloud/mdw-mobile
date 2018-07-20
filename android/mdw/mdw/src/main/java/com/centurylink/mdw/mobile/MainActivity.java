/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.centurylink.mdw.mobile.app.BadSettingsException;
import com.centurylink.mdw.mobile.app.Reporter;
import com.centurylink.mdw.mobile.app.Settings;
import com.centurylink.mdw.mobile.model.App;
import com.centurylink.mdw.mobile.model.NavDrawerItem;

import org.json.JSONArray;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
          TabLayout.OnTabSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BLANK = "file:///android_asset/blank.html";

    private Settings settings;
    private Toolbar toolbar;

    private String appId;

    private WebView webView;
    private WebView getWebView() { return webView; }

    private ImageView loadingView;
    private AnimationDrawable loadingAnimation;

    private TabLayout tabLayout;
    private NavigationView navigationView;

    private NavDrawerItem currentNavDrawerItem;
    private int navMenuGroupId = 0;

    private MenuItem appEnvMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new Settings(getApplicationContext());
        App.Env env = settings.getEnv();
        appId = env == null ? null : env.getApp().getId();
        String token = appId == null ? null : settings.getToken(appId);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setOnTabSelectedListener(this);

        loadingView = (ImageView) findViewById(R.id.web_loading);
        loadingView.setBackgroundResource(R.drawable.loading);
        loadingAnimation = (AnimationDrawable)loadingView.getBackground();

        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (settings.getToken(appId) == null || App.isLogin(url)) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    return true;
                }
                else {
                    view.loadUrl(url, settings.getRequestHeaders(appId));
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showLoading(false);
                reflectUrlInNavDrawerAndTab(url);
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDomStorageEnabled(true);

        if (BuildConfig.DEBUG) {
            // allow debugging with chrome dev tools
            WebView.setWebContentsDebuggingEnabled(true);

            // do not cache in debug
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.getSettings().setAppCacheEnabled(false);
            webView.clearCache(true);
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.Env env = settings.getEnv();
        appId = env == null ? null : env.getApp().getId();
        String token = appId == null ? null : settings.getToken(appId);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        else {
            showLoading(true);
            initNavDrawer();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setTabs(false);
    }

    private void showAppListDialog() {
        AppListDialog appListDialog = new AppListDialog();
        appListDialog.setEnvSelectListener(new AppListDialog.EnvSelectListener() {
            @Override
            public void onEnvSelect(App.Env env) {
                webView.clearView();
                webView.loadUrl("about:blank");
                settings.setEnv(env);
                appId = env.getApp().getId();
                String title = "";
                if (!env.getName().startsWith("prod") && !env.getName().startsWith("Prod"))
                    title += "(" + env.getName() + ")";
                appEnvMenuItem.setTitle(title);
                String token = env == null ? null : settings.getToken(env.getApp().getId());
                if (token == null) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("appId", appId);
                    startActivity(intent);
                    finish();
                }
                else {
                    onResume();
                }
            }
        });
        appListDialog.show(getFragmentManager());
    }

    private void initNavDrawer() {
        try {
            if (settings.getNavDrawerItems() == null) {
                new RetrieveNavTabs().execute();
            } else {
                addNavDrawerItems();
            }
        } catch (BadSettingsException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            Snackbar.make(webView, ex.toString(), Snackbar.LENGTH_LONG).show();
            if (settings.isErrorReportingEnabled())
                new Reporter(ex).send();
        }
    }

    private void addNavDrawerItems() {
        Menu navMenu = navigationView.getMenu();
        if (navMenuGroupId == 0) {
            MenuItem placeholder = navMenu.getItem(0);
            navMenuGroupId = placeholder.getGroupId();
        }

        // remove dynamic nav items
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < navMenu.size(); i++) {
            if (navMenu.getItem(i).getGroupId() == navMenuGroupId)
                toRemove.add(navMenu.getItem(i).getItemId());
        }
        for (Integer id : toRemove) {
            navMenu.removeItem(id);
        }
        int index = 0;
        for (String label : settings.getNavDrawerItems().keySet()) {
            NavDrawerItem navDrawerItem = settings.getNavDrawerItems().get(label);
            navDrawerItem.setIndex(index);
            MenuItem item = navMenu.add(navMenuGroupId, Menu.NONE, Menu.FIRST, navDrawerItem.getLabel());
            String icon = navDrawerItem.getIcon();
            if (icon != null) {
                int lastDot = icon.lastIndexOf('.');
                if (lastDot > 0)
                    icon = icon.substring(0, lastDot);
                int iconId = getResourceId(icon, "drawable");
                if (iconId > 0) {
                    item.setIcon(iconId);
                }
            }
            index++;
        }

        // refreshing navigation drawer adapter
        for (int i = 0, count = navigationView.getChildCount(); i < count; i++) {
            final View child = navigationView.getChildAt(i);
            if (child != null && child instanceof ListView) {
                ListView menuView = (ListView) child;
                HeaderViewListAdapter adapter = (HeaderViewListAdapter) menuView.getAdapter();
                BaseAdapter wrapped = (BaseAdapter) adapter.getWrappedAdapter();
                wrapped.notifyDataSetChanged();
            }
        }

        restoreCurrentNavDrawer();
    }

    private void restoreCurrentNavDrawer() {
        String navDrawer = null;
        Iterator<NavDrawerItem> iterator = settings.getNavDrawerItems().values().iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            NavDrawerItem navDrawerItem = iterator.next();
            if (i == 0 || navDrawerItem.getLabel().equals(settings.getCurrentNavDrawer()))
                navDrawer = navDrawerItem.getLabel();
        }
        setCurrentNavDrawer(navDrawer);
    }

    private void restoreCurrentTab(boolean load) {
        int tabIndex = 0;
        for (int i = 0; i < currentNavDrawerItem.getLinks().size(); i++) {
            NavDrawerItem.Link link = currentNavDrawerItem.getLinks().get(i);
            if (link.getLabel().equals(settings.getCurrentTab())) {
                tabIndex = i;
                break;
            }
        }
        loadSelectedTab = load;
        if (tabIndex + 1 > tabLayout.getTabCount()) {
            // can happen if orientation change makes fewer tabs available -- force load
            tabIndex = 0;
            loadSelectedTab = true;
        }
        tabLayout.getTabAt(tabIndex).select();
        loadSelectedTab = true;
    }

    /**
     * These change depending on the selected currentNavDrawerItem
     */
    private void setTabs(boolean loadCurrent) {
        tabLayout.removeAllTabs();
        int max = 3;
        if (settings.isLandscape() || settings.isTablet())
            max = 5;
        if (settings.isLandscape() && settings.isTablet())
            max = 7;
        loadSelectedTab = false;
        for (int i = 0; i < currentNavDrawerItem.getLinks().size() && i < max; i++) {
            NavDrawerItem.Link link = currentNavDrawerItem.getLinks().get(i);
            tabLayout.addTab(tabLayout.newTab().setText(link.getLabel()));
        }
        loadSelectedTab = true;
        restoreCurrentTab(loadCurrent);
    }

    public int getResourceId(String name, String type) {
        return getResources().getIdentifier(name, type, getPackageName());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        appEnvMenuItem = menu.findItem(R.id.action_app_env);
        App.Env env = settings.getEnv();
        if (env != null) {
            String title = "";
            if (!env.getName().startsWith("prod") && !env.getName().startsWith("Prod"))
                title += "(" + env.getName() + ")";
            appEnvMenuItem.setTitle(title);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_app_env || id == R.id.action_exit) {
            showAppListDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.docs_help) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Settings.HELP_DOCS_URL)));
        }
        else if (item.getItemId() == R.id.docs_site) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Settings.MDW_SITE_URL)));
        }
        else {
            settings.setCurrentTab(null);
            navigationView.getMenu().getItem(currentNavDrawerItem.getIndex()).setChecked(false);
            String title = item.getTitle().toString();
            setCurrentNavDrawer(title);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setCurrentNavDrawer(String title) {
        if (currentNavDrawerItem != null)
            navigationView.getMenu().getItem(currentNavDrawerItem.getIndex()).setChecked(false);
        settings.setCurrentNavDrawer(title);
        currentNavDrawerItem = settings.getNavDrawerItems().get(title);
        navigationView.getMenu().getItem(currentNavDrawerItem.getIndex()).setChecked(true);
        toolbar.setTitle(currentNavDrawerItem.getLabel());
        setTabs(true);
    }

    private boolean loadSelectedTab;

    /**
     * This is the sub-page selected.
     * @param tab
     */
    public void onTabSelected(TabLayout.Tab tab) {
        String label = tab.getText().toString();
        if (loadSelectedTab) {
            loadTab(label);
        }
    }
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    private void loadTab(String tab) {
        settings.setCurrentTab(tab);
        NavDrawerItem.Link link = currentNavDrawerItem.getLink(tab);
        loadHref(link.getHref());
    }

    private void loadHref(String path) {
        webView.loadUrl("file:///android_asset/loading.html");
        String url = settings.getEnv().getUrl() + "/" + path;
        Log.i(TAG, "Loading URL: " + url);
        webView.loadUrl(url, settings.getRequestHeaders(appId));
    }

    private void reflectUrlInNavDrawerAndTab(String url) {
        NavDrawerItem.Link link = findLinkForUrl(url);
        NavDrawerItem navDrawerItem = link == null ? null : link.getNavDrawerItem();
        if (link != null && (!navDrawerItem.getLabel().equals(settings.getCurrentNavDrawer())
                || !link.getLabel().equals(settings.getCurrentTab()))) {
            // update nav drawer (and reload tabs)
            if (!navDrawerItem.getLabel().equals(settings.getCurrentNavDrawer())) {
                setCurrentNavDrawer(navDrawerItem.getLabel());
            }
            settings.setCurrentTab(link.getLabel());
            loadSelectedTab = false;
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab.getText().equals(link.getLabel()))
                    tab.select();
            }
            loadSelectedTab = true;
        }
    }

    private NavDrawerItem.Link findLinkForUrl(String url) {
        if (settings.getNavDrawerItems() != null) {
            for (NavDrawerItem navDrawerItem : settings.getNavDrawerItems().values()) {
                for (NavDrawerItem.Link link : navDrawerItem.getLinks()) {
                    if (url.equals(settings.getEnv().getUrl() + "/" + link.getHref())) {
                        return link;
                    }
                }
            }
        }
        return null;
    }

    private void showLoading(final boolean show) {
        int t = getResources().getInteger(android.R.integer.config_shortAnimTime);

        webView.setVisibility(show ? View.GONE : View.VISIBLE);
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show)
            loadingAnimation.start();
        else
            loadingAnimation.stop();
    }

    private class RetrieveNavTabs extends AsyncTask<URL,Integer,Long> {

        private Exception ex;

        protected Long doInBackground(URL... urls) {
            Long before = null;
            try {
                // retrieve the tabs from hub
                OkHttpClient client = new OkHttpClient();
                String url = settings.getEnv().getUrl() + "/js/nav.json";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String responseText = response.body().string();
                JSONArray arr = new JSONArray(responseText);
                Map<String,NavDrawerItem> navDrawerItems = new LinkedHashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    NavDrawerItem navDrawerItem = new NavDrawerItem(arr.getJSONObject(i));
                    navDrawerItems.put(navDrawerItem.getLabel(), navDrawerItem);
                }
                settings.setNavDrawerItems(navDrawerItems);

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (settings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                showLoading(false);
            } else {
                addNavDrawerItems();
            }
        }
    }

}
