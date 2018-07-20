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

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.centurylink.mdw.mobile.app.MdwApp;
import com.centurylink.mdw.mobile.app.Settings;
import com.centurylink.mdw.mobile.model.App;

import java.util.List;

public class AppListDialog extends DialogFragment {

    private TextView explainText;
    private ExpandableListView listView;
    private AppListAdapter listAdapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_app_list, null);
        builder.setView(view);
        builder.setIcon(R.drawable.hub_logo);
        builder.setTitle("App Environments");
        final Settings settings = new Settings(MdwApp.getAppContext());
        if (settings.getEnv() != null) {
            builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
        }
        builder.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.logout();
                startActivity(new Intent(MdwApp.getAppContext(), LoginActivity.class));
                dismiss();
            }
        });

        explainText = (TextView) view.findViewById(R.id.app_list_explain);
        listView = (ExpandableListView) view.findViewById(R.id.app_list);
        List<App> apps = settings.getApps();
        if (apps.isEmpty()) {
            listView.setVisibility(View.GONE);
            explainText.setText("It appears you don't have any apps yet.  Head over to mdw-central.com to create a new app or request access to an existing one.");
            explainText.setVisibility(View.VISIBLE);
        }
        else {
            int envCount = 0;
            for (App app : apps) {
                for (App.Env env : app.getEnvs()) {
                    if (env.getUrl() != null)
                        envCount += app.getEnvs().size();
                }
            }
            if (envCount == 0) {
                explainText.setText("No accessible envs found.  Head over to mdw-central.com to create an app env, and make sure to give it a valid MDW endpoint URL.");
                explainText.setVisibility(View.VISIBLE);
            }
            listAdapter = new AppListAdapter(this, apps, settings.getEnv());
            listView.setAdapter(listAdapter);
            for (int i = 0; i < apps.size(); i++)
                listView.expandGroup(i);
        }
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager, null);
    }

    public interface EnvSelectListener {
        public void onEnvSelect(App.Env env);
    }

    private EnvSelectListener envSelectListener;
    public EnvSelectListener getEnvSelectListener() {
        return envSelectListener;
    }
    public void setEnvSelectListener(EnvSelectListener envSelectListener) {
        this.envSelectListener = envSelectListener;
    }

    public void selectEnv(final App.Env env) {
        listAdapter.notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (envSelectListener != null)
                    envSelectListener.onEnvSelect(env);
                getDialog().dismiss();
            }
        }, 250);
    }
}