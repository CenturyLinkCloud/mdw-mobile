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
package com.centurylink.mdw.mobile.app;

import android.util.Log;

import com.centurylink.mdw.mobile.model.App;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Report errors to mdw-central.  Fails silently except to log.
 */
public class Reporter {
    private static final String TAG = Reporter.class.getSimpleName();

    private Throwable throwable;
    private String message;
    private Settings settings;

    public Reporter(Throwable t) {
        this.throwable = t;
        this.message = t.getMessage();
        settings = new Settings(MdwApp.getAppContext());
    }

    public Reporter(String message) {
        this.message = message;
    }

    private String getUrl() {
        return settings.getCentralUrl() + "/api/errors";
    }

    /**
     * Report the error in a background thread.
     */
    public void send() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject json = buildJson();
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(Settings.DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(Settings.DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                            .build();

                    RequestBody body = RequestBody.create(JSON, json.toString(2));
                    Request request = new Request.Builder()
                            .url(getUrl())
                            .addHeader("mdw-app-token", Secrets.MDW_MOBILE_TOKEN)
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException("Bad auth response: " + response.body().string());
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }
        }).start();
    }

    private JSONObject buildJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject error = new JSONObject();
        json.put("error", error);
        error.put("source", "mdw-mobile v" + settings.getMdwMobileVersion() +
                " (android " + settings.getAndroidVersion() + ")");
        error.put("message", message == null ? "No message" : message);
        if (throwable != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(out));
            error.put("stackTrace", new String(out.toByteArray()));
        }
        return json;
    }
}
