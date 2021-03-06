package io.indoorlocation.navisens.demoapp;

import android.app.Application;

import io.mapwize.mapwizeformapbox.AccountManager;

public class DemoApplication extends Application {

    static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoidmFsZGkxMSIsImEiOiJjam4ydmFlcmEwbzJpM3FvOG80aW00YnhoIn0.kQh9_JFx0H1vuR-Cu6b8Qw";
    static final String MAPWIZE_STYLE_URL_BASE = "http://outdoor.mapwize.io/styles/mapwize/style.json?key=";
    static final String NAVISENS_API_KEY = "jAKCbUXq0tW1slgWfkFZwzCsrAPGe2Kyq1LZDz60RNclFGCLO4AKphJVkdk0lL3o";
    static final String MAPWIZE_ACCESS_KEY = "4b6135da473ec6b301766fc2f7378677";

    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.start(this, MAPWIZE_ACCESS_KEY);
    }

}