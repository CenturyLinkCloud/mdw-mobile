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

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.centurylink.mdw.mobile.model.App;

import java.util.List;

public class AppListAdapter extends BaseExpandableListAdapter {

    private AppListDialog dialog;
    private List<App> apps;

    // selected environment
    private App.Env env;

    private boolean selectable = true;

    public AppListAdapter(AppListDialog dialog, List<App> apps, App.Env env) {
        this.dialog = dialog;
        this.apps = apps;
        this.env = env;
    }

    @Override
    public Object getChild(int appPosition, int envPosition) {
        return apps.get(appPosition).getEnvs().get(envPosition);
    }

    @Override
    public long getChildId(int appPosition, int envPosition) {
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final App.Env env = (App.Env) getChild(groupPosition, childPosition);
        final TextView text = (TextView) dialog.getDialog().getLayoutInflater().inflate(R.layout.env_item, null);
        text.setTextColor(Color.parseColor("#0277bd"));
        text.setText(env.getName());
        if (this.env != null && this.env.getApp().getId().equals(env.getApp().getId())
                && this.env.getName().equals(env.getName())) {
            text.setTypeface(null, Typeface.BOLD);
        }
        text.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectable) {
                    selectable = false;
                    AppListAdapter.this.env = env;
                    dialog.selectEnv(env);
                }
            }
        });
        return text;
    }

    @Override
    public int getChildrenCount(int appPosition) {
        return apps.get(appPosition).getEnvs().size();
    }

    @Override
    public Object getGroup(int appPosition) {
        return apps.get(appPosition);
    }

    @Override
    public int getGroupCount() {
        return apps.size();
    }

    @Override
    public long getGroupId(int appPosition) {
        return 0;
    }

    @Override
    public View getGroupView(int appPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        CheckedTextView checkedTextView = (CheckedTextView) dialog.getDialog().getLayoutInflater().inflate(R.layout.app_item, null);
        App app = (App) getGroup(appPosition);
        checkedTextView.setText(app.getId());
        checkedTextView.setChecked(isExpanded);
        return checkedTextView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int appPosition, int envPosition) {
        return false;
    }
}