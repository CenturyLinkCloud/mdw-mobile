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
package com.centurylink.mdw.mobile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.centurylink.mdw.mobile.app.Reporter;
import com.centurylink.mdw.mobile.app.Secrets;
import com.centurylink.mdw.mobile.app.Settings;
import com.centurylink.mdw.mobile.model.App;

import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private Settings settings;

    private View loginForm;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private View progress;
    private TextView signupLink;
    private String username;
    private String password;
    private String appId;

    private AsyncTask<Void,Void,Boolean> authTask = null;
    private TextWatcher textWatcher = new TextWatcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.icon);

        settings = new Settings(getApplicationContext());
        String username = settings.getUsername();

        usernameText = (EditText) findViewById(R.id.username);
        if (username != null)
            usernameText.setText(username);
        usernameText.addTextChangedListener(textWatcher);

        passwordText = (EditText) findViewById(R.id.password);
        passwordText.addTextChangedListener(textWatcher);
        passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    String user = usernameText.getText().toString();
                    String password = passwordText.getText().toString();
                    authenticate(user, password);
                    return true;
                }
                return false;
            }
        });

        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameText.getText().toString();
                String password = passwordText.getText().toString();
                if (username != null && !username.isEmpty() && password != null && !password.isEmpty())
                    authenticate(username, password);
            }
        });

        signupLink = (TextView) findViewById(R.id.signup);
        signupLink.setTextColor(Color.parseColor("#0277bd"));
        signupLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signupLink.setTextColor(Color.parseColor("#4fc3f7"));
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(settings.getCentralUrl() + "/signup")));
            }
        });
        loginForm = findViewById(R.id.login_form);
        progress = findViewById(R.id.login_progress);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void authenticate(String user, String password) {
        if (authTask != null) {
            return;
        }
        this.username = user;
        this.password = password;
        this.appId = getIntent().getStringExtra("appId");
        showProgress(true);
        if (appId == null) {
            settings.logout();
            authTask = new AuthenticationTask();
            authTask.execute((Void)null);
        }
        else {
            settings.setToken(appId, null);
            authTask = new AppAuthTask();
            authTask.execute((Void)null);
        }
    }

    private void showProgress(final boolean show) {
        int t = getResources().getInteger(android.R.integer.config_shortAnimTime);

        loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        loginForm.animate().setDuration(t).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        progress.animate().setDuration(t).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progress.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private class TextWatcher implements android.text.TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        public void afterTextChanged(Editable s) {
            String u = usernameText.getText().toString();
            String p = passwordText.getText().toString();
            boolean enabled = !u.isEmpty() && !p.isEmpty();
            loginButton.setEnabled(enabled);
            loginButton.setTextColor(enabled ? Color.WHITE : Color.LTGRAY);
        }
    }

    private class AuthenticationTask extends AsyncTask<Void,Void,Boolean> {

        private Exception ex;

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, "Authenticating user '" + username + "'");
            try {
                // authenticate for app mdw-mobile via mdw-central
                String centralUrl = settings.getCentralUrl() + "/api";
                JSONObject json = new JSONObject();
                json.put("user", username);
                json.put("password", password);
                json.put("appId", "mdw-mobile");
                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(Settings.DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(Settings.DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(JSON, json.toString(2));
                Request request = new Request.Builder()
                        .url(centralUrl + "/auth")
                        .addHeader("mdw-app-token", Secrets.MDW_MOBILE_TOKEN)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    JSONObject responseJson = new JSONObject(response.body().string());
                    String userToken = responseJson.getString("mdwauth");
                    settings.setToken("mdw-mobile", userToken);
                    settings.setUsername(username);
                    // retrieve apps for user
                    request = new Request.Builder()
                            .url(centralUrl + "/apps?user=" + username)
                            .addHeader("mdw-app-token", Secrets.MDW_MOBILE_TOKEN)
                            .addHeader("Authorization", "Bearer " + userToken)
                            .get()
                            .build();
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        List<App> apps = App.parseApps(new JSONObject(response.body().string()));
                        settings.setApps(apps);
                    }
                    else {
                        throw new IOException("Bad apps retrieval response: " + response);
                    }
                }
                else {
                    throw new IOException("Bad auth response: " + response);
                }
                return true;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (settings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            authTask = null;
            if (success) {
                AppListDialog appListDialog = new AppListDialog();
                appListDialog.setEnvSelectListener(new AppListDialog.EnvSelectListener() {
                    @Override
                    public void onEnvSelect(App.Env env) {
                        settings.setEnv(env);
                        appId = env.getApp().getId();
                        authTask = new AppAuthTask();
                        authTask.execute((Void)null);
                    }
                });
                appListDialog.show(getFragmentManager());

            } else {
                showProgress(false);
                String msg = "Authentication failure";
                if (this.ex instanceof UnknownHostException)
                    msg += " (are you offline?)";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                passwordText.setText("");
            }
        }

        @Override
        protected void onCancelled() {
            authTask = null;
            showProgress(false);
        }
    }

    /**
     * Authenticate for selected app.
     */
    class AppAuthTask extends AsyncTask<Void,Void,Boolean> {

        private Exception ex;

        @Override
        protected Boolean doInBackground(Void... params) {
            String user = settings.getUsername();
            Log.i(TAG, "Authenticating user '" + user + "' for app: " + appId);
            try {
                // authenticate for app via mdw-central
                JSONObject json = new JSONObject();
                json.put("user", user);
                json.put("password", password);
                json.put("appId", appId);
                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(Settings.DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(Settings.DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                        .build();
                RequestBody body = RequestBody.create(JSON, json.toString(2));
                Request request = new Request.Builder()
                        .url(settings.getCentralUrl() + "/api/auth")
                        .addHeader("mdw-app-token", Secrets.MDW_MOBILE_TOKEN)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    JSONObject responseJson = new JSONObject(response.body().string());
                    String userToken = responseJson.getString("mdwauth");
                    settings.setToken(appId, userToken);
                }
                else {
                    throw new IOException("Bad auth response: " + response);
                }
                return true;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                settings.setEnv(null);
                if (settings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            authTask = null;
            if (success) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                settings.setToken(appId, null);
                showProgress(false);
                Toast.makeText(getApplicationContext(), "Auth failure for " + appId, Toast.LENGTH_LONG).show();
                passwordText.setText("");
            }
        }

        @Override
        protected void onCancelled() {
            authTask = null;
            showProgress(false);
        }
    }
}
