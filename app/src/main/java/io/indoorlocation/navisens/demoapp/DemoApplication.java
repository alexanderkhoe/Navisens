package io.indoorlocation.navisens.demoapp;

import android.app.Application;

import io.mapwize.mapwizeformapbox.AccountManager;

public class DemoApplication extends Application {

    static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoidmFsZGkxMSIsImEiOiJjam4ydmFlcmEwbzJpM3FvOG80aW00YnhoIn0.kQh9_JFx0H1vuR-Cu6b8Qw";
    static final String MAPWIZE_API_KEY = "1f04d780dc30b774c0c10f53e3c7d4ea";
    static final String MAPWIZE_STYLE_URL_BASE = "http://outdoor.mapwize.io/styles/mapwize/style.json?key=";
    static final String NAVISENS_API_KEY = "jAKCbUXq0tW1slgWfkFZwzCsrAPGe2Kyq1LZDz60RNclFGCLO4AKphJVkdk0lL3o";


    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.start(this, MAPWIZE_API_KEY);
    }

}