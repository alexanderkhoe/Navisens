package io.indoorlocation.navisens.demoapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.navisens.NavisensIndoorLocationProvider;
import io.mapwize.mapwizeformapbox.AccountManager;
import io.mapwize.mapwizeformapbox.MapOptions;
import io.mapwize.mapwizeformapbox.MapwizePlugin;

public class MapActivity extends AppCompatActivity{

    private MapView mapView;
    private MapwizePlugin mapwizePlugin;
    private IndoorLocationProvider manualIndoorLocationProvider;
    private NavisensIndoorLocationProvider navisensIndoorLocationProvider;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private String currLat;
    private String currLon;

    public TextView currentLat;
    public TextView currentLon;

    private String getLat;
    private String getLon;

    private double lat, lon;

    private FusedLocationProviderClient mFusedLocationClient;

    String LatLon;
    private TextView sharedLat;
    private TextView sharedLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, DemoApplication.MAPBOX_ACCESS_TOKEN);
        setContentView(R.layout.activity_map);
        currentLat = findViewById(R.id.currentLat);
        currentLon = findViewById(R.id.currentLon);
        sharedLat = findViewById(R.id.currSharedLat);
        sharedLon = findViewById(R.id.currSharedLon);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl(DemoApplication.MAPWIZE_STYLE_URL_BASE + AccountManager.getInstance().getApiKey());
        MapOptions opts = new MapOptions.Builder().build();
        mapwizePlugin = new MapwizePlugin(mapView, opts);
        mapwizePlugin.setOnDidLoadListener(plugin -> {
            requestLocationPermission();

        });

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getCurrLocation();
                        getSharedLocation();
                        try {
                            Thread.sleep(800);
                            LatLon = " ";
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 0, 1000);

    }

    protected void getCurrLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currLat = Double.toString(location.getLatitude());
                            currLon = Double.toString(location.getLongitude());
                            currentLat.setText(currLat);
                            currentLon.setText(currLon);
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        }
                    }
                });
    }

    protected void getSharedLocation(){
//        navisensIndoorLocationProvider.receiveNetworkData(motionDna);//null object reference. coba debug
//        navisensIndoorLocationProvider.receiveNetworkData(MotionDna.NetworkCode.RAW_NETWORK_DATA, map);
        try{
            LatLon = navisensIndoorLocationProvider.getSharedLoc();
            String[] parts = LatLon.split(":")[1].split(",");
            getLat = parts[0];
            getLon = parts[1];
            sharedLat.setText(getLat);
            sharedLon.setText(getLon);
        }
        catch(NullPointerException ex){
            ex.printStackTrace();
        }

    }

    private void setupLocationProvider() {
        manualIndoorLocationProvider = new ManualIndoorLocationProvider();
        navisensIndoorLocationProvider = new NavisensIndoorLocationProvider(getApplicationContext(),
                DemoApplication.NAVISENS_API_KEY, manualIndoorLocationProvider);
        mapwizePlugin.setLocationProvider(navisensIndoorLocationProvider);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupLocationProvider();
                }
            }
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            setupLocationProvider();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
