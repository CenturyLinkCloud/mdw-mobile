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

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Populated from a "tab" object in JSON from server.
 */
public class NavDrawerItem {
    public NavDrawerItem(JSONObject json) throws JSONException {
        if (json.has("id"))
            this.id = json.getString("id");
        if (json.has("label"))
            this.label = json.getString("label");
        if (json.has("icon"))
            this.icon = json.getString("icon");
        if (json.has("url"))
            this.url = json.getString("url");
        if (json.has("routes")) {
            this.routes = new ArrayList<>();
            JSONArray routesArr = json.getJSONArray("routes");
            for (int i = 0; i < routesArr.length(); i++) {
                this.routes.add(routesArr.getString(i));
            }
        }
        if (json.has("navs")) {
            this.links = new ArrayList<>();
            JSONArray navsArr = json.getJSONArray("navs");
            for (int i = 0; i < navsArr.length(); i++) {
                JSONObject navJson = navsArr.getJSONObject(i);
                if (navJson.has("links")) {
                    JSONArray linksArr = navJson.getJSONArray("links");
                    for (int j = 0; j < linksArr.length(); j++) {
                        this.links.add(new Link(i+j, linksArr.getJSONObject(j)));
                    }
                }
            }
            Collections.sort(this.links);
        }
        if (json.has("condition"))
            this.condition = json.getString("condition");
    }

    private int index;
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String label;
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    private String icon;
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    private List<String> routes;
    public List<String> getRoutes() { return routes; }
    public void setRoutes(List<String> routes) { this.routes = routes; }

    private String condition;
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    private List<Link> links;
    public List<Link> getLinks() { return links; }
    public void setLinks(List<Link> links) { this.links = links; }

    public Link getLink(String label) {
        for (Link link : links) {
            if (link.getLabel().equals(label))
                return link;
        }
        return null;
    }

    public class Link implements Comparable<Link> {
        public Link(int index, JSONObject json) throws JSONException {
            this.index = index;
            if (json.has("label"))
                this.label = json.getString("label");
            if (json.has("path"))
                this.path = json.getString("path");
            if (json.has("href"))
                this.href = json.getString("href");
            if (json.has("target"))
                this.target = json.getString("target");
            if (json.has("priority"))
                this.priority = json.getInt("priority");
        }

        final public int index;

        private String label;
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        private String path;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        private String href;
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }

        private String target;
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        private int priority;
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }

        public NavDrawerItem getNavDrawerItem() {
            return NavDrawerItem.this;
        }

        /**
         * Order by priority, index.
         */
        @Override
        public int compareTo(@NonNull Link other) {
            if (this.priority != other.priority) {
                if (other.priority == 0)
                    return -10;
                else if (this.priority == 0)
                    return 10;
                else
                    return this.priority - other.priority;
            }
            else {
                return this.index - other.index;
            }
        }
    }
}
