package io.indoorlocation.navisens.demoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.navisens.motiondnaapi.MotionDna;

import java.util.Timer;
import java.util.TimerTask;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.navisens.NavisensIndoorLocationProvider;
import io.mapwize.mapwizeformapbox.AccountManager;
import io.mapwize.mapwizeformapbox.MapOptions;
import io.mapwize.mapwizeformapbox.MapwizePlugin;
import io.mapwize.mapwizeformapbox.api.Api;
import io.mapwize.mapwizeformapbox.api.ApiCallback;
import io.mapwize.mapwizeformapbox.model.ParsedUrlObject;

public class MapActivity extends AppCompatActivity{

    private MapView mapView;
    private MapwizePlugin mapwizePlugin;
    private IndoorLocationProvider manualIndoorLocationProvider;
    private NavisensIndoorLocationProvider navisensIndoorLocationProvider;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private String currLat;
    private String currLon;

    public TextView currentLat;
    public TextView currentLon;

    private String getLat;
    private String getLon;

    private double lat, lon;

    private FusedLocationProviderClient mFusedLocationClient;

    private TextView sharedLat;
    private TextView sharedLon;

    MotionDna.LocationStatus locationStat1 = MotionDna.LocationStatus.NAVISENS_INITIALIZED;

    private MapboxMap mapboxMap;
    private boolean startedFromUrl = false;
    private IndoorLocation lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, DemoApplication.MAPBOX_ACCESS_TOKEN);
        setContentView(R.layout.activity_map);
        currentLat = findViewById(R.id.currentLat);
        currentLon = findViewById(R.id.currentLon);
        sharedLat = findViewById(R.id.currSharedLat);
        sharedLon = findViewById(R.id.currSharedLon);
        Button camBtn = findViewById(R.id.btn_cam);
        Intent intent = MapActivity.this.getIntent();
        String url = null;
        if (intent.getStringExtra("mapwizeUrl") != null) {
            url = intent.getStringExtra("mapwizeUrl");
        }
        else if (intent.getDataString() != null) {
            url = intent.getDataString();
        }
        if (url != null) {
            startedFromUrl = true;
            Api.getParsedUrlObject(url, new ApiCallback<ParsedUrlObject>() {
                @Override
                public void onSuccess(final ParsedUrlObject object) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            handleParsedUrl(object);
                        }
                    };
                    uiHandler.post(runnable);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl(DemoApplication.MAPWIZE_STYLE_URL_BASE + AccountManager.getInstance().getApiKey());
        MapOptions opts = new MapOptions.Builder().venueId("5bc4474c731e630012b7607c").build();
        mapwizePlugin = new MapwizePlugin(mapView, opts);
        mapwizePlugin.setOnDidLoadListener(plugin -> {
            requestLocationPermission();
        });
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mMap) {
                mapboxMap = mMap;
            }
        });

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    getCurrLocation();
                    getSharedLocation();
                });
            }
        }, 0, 1000);
        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapActivity.this, ScannerActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(this,"Scanning cancelled", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                String[] barcodeScanned = result.getContents().split(",");

            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleParsedUrl(final ParsedUrlObject parsedUrlObject) {
        if (parsedUrlObject.getAccessKey() != null) {
            Api.getAccess(parsedUrlObject.getAccessKey(), new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean access) {
                    parsedUrlObject.setAccessKey(null);
                    mapwizePlugin.refresh(new MapwizePlugin.OnAsyncTaskReady() {
                        @Override
                        public void ready() {
                            Handler uiHandler = new Handler(Looper.getMainLooper());
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    handleParsedUrl(parsedUrlObject);
                                }
                            };
                            uiHandler.post(runnable);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
            return;
        }

        if (parsedUrlObject.getIndoorLocation() != null) {
            if (navisensIndoorLocationProvider != null) {
                navisensIndoorLocationProvider.setIndoorLocation(parsedUrlObject.getIndoorLocation());
            }
            else {
                lastLocation = parsedUrlObject.getIndoorLocation();
            }
        }

        if (parsedUrlObject.getLanguage() != null) {
            mapwizePlugin.setPreferredLanguage(parsedUrlObject.getLanguage());
        }

        if (parsedUrlObject.getUniverse() != null) {
            mapwizePlugin.setUniverseForVenue(parsedUrlObject.getUniverse(), null);
        }

        Double zoom = parsedUrlObject.getZoom();
        LatLngBounds bounds = parsedUrlObject.getBounds();
        if (bounds.getSouthWest().equals(bounds.getNorthEast())) {
            CameraUpdate nextPosition = CameraUpdateFactory.newLatLngZoom(bounds.getSouthWest(), zoom==null?20:zoom);
            mapboxMap.easeCamera(nextPosition);

        }
        else {
            CameraUpdate nextPosition = CameraUpdateFactory.newLatLngBounds(bounds, 10);
            CameraPosition nextCameraPosition = nextPosition.getCameraPosition(this.mapboxMap);
            if (nextCameraPosition != null && nextCameraPosition.zoom < 16) {
                nextPosition = CameraUpdateFactory.newLatLngZoom(nextCameraPosition.target, 16);
            }
            if (nextCameraPosition != null && zoom != null) {
                nextPosition = CameraUpdateFactory.newLatLngZoom(nextCameraPosition.target, zoom);
            }
        }
        if (parsedUrlObject.getFloor() != null) {
            mapwizePlugin.setFloor(parsedUrlObject.getFloor());
        }
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
        if(MotionDna.LocationStatus.NAVISENS_INITIALIZED==locationStat1) {
            try {
                String LatLon = navisensIndoorLocationProvider.sharedLoc;
                String[] parts = LatLon.split(" ");
                getLat = parts[0];
                getLon = parts[1];
                sharedLat.setText(getLat);
                sharedLon.setText(getLon);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
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
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)&& ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
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
