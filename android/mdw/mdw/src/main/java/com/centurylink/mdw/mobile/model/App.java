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
package com.centurylink.mdw.mobile.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final String TAG = App.class.getSimpleName();

    public static final boolean isLogin(String url) {
        int hash = url.indexOf('#');
        if (hash > 0)
            url = url.substring(0, hash);
        int q = url.indexOf('?');
        if (q > 0)
            url = url.substring(0, q);
        return url.endsWith("/login");
    }

    public static final List<App> parseApps(JSONObject json) throws JSONException, MalformedURLException {
        List<App> apps = new ArrayList<>();
        JSONArray appsArr = json.getJSONArray("apps");
        for (int i = 0; i < appsArr.length(); i++) {
            JSONObject appJson = appsArr.getJSONObject(i);
            apps.add(new App(appJson));
        }
        return apps;
    }

    public static final String stringifyApps(List<App> apps) throws JSONException {
        return stringifyApps(apps, 0);
    }

    public static final String stringifyApps(List<App> apps, int indent) throws JSONException {
        JSONObject appsJson = new JSONObject();
        JSONArray appsArr = new JSONArray();
        appsJson.put("apps", appsArr);
        for (App app : apps) {
            appsArr.put(app.toJson());
        }
        return appsJson.toString(indent);
    }

    private String id;
    public String getId() { return id; }

    private String name;
    public String getName() { return name; }

    private List<Env> envs = new ArrayList<>();
    public List<Env> getEnvs() { return envs; }

    public App(JSONObject json) throws JSONException, MalformedURLException {
        if (json.has("id"))
            this.id = json.getString("id");
        if (json.has("name"))
            this.name = json.getString("name");
        if (json.has("envs")) {
            JSONArray envsArr = json.getJSONArray("envs");
            for (int i = 0; i < envsArr.length(); i++) {
                this.addEnv(envsArr.getJSONObject(i));
            }
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (this.id != null)
            json.put("id", this.id);
        if (this.name != null)
            json.put("name", this.name);
        if (this.envs != null) {
            JSONArray envsArr = new JSONArray();
            json.put("envs", envsArr);
            for (Env env : this.envs) {
                envsArr.put(env.toJson());
            }
        }
        return json;
    }

    public void addEnv(JSONObject json) throws JSONException, MalformedURLException {
        getEnvs().add(new Env(json));
    }

    public class Env {
        private String name;
        public String getName() { return name; };

        private URL url;
        public URL getUrl() { return url; }

        public Env(JSONObject json) throws JSONException {
            if (json.has("name"))
                this.name = json.getString("name");
            if (json.has("url")) {
                try {
                    this.url = new URL(json.getString("url"));
                }
                catch (MalformedURLException ex) {
                    Log.e(TAG, "Bad URL: '" + json.getString("url") + "'");
                }
            }
        }

        public App getApp() {
            return App.this;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            if (this.name != null)
                json.put("name", this.name);
            if (this.url != null)
                json.put("url", this.url);
            return json;
        }

    }
}
