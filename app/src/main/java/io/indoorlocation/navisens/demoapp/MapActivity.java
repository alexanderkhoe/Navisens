package io.indoorlocation.navisens.demoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaInterface;

import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.navisens.NavisensIndoorLocationProvider;
import io.mapwize.mapwizeformapbox.AccountManager;
import io.mapwize.mapwizeformapbox.FollowUserMode;
import io.mapwize.mapwizeformapbox.MapOptions;
import io.mapwize.mapwizeformapbox.MapwizePlugin;
import io.mapwize.mapwizeformapbox.model.Venue;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

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

    private TextView sharedLat;
    private TextView sharedLon;

    MotionDna.LocationStatus locationStat1 = MotionDna.LocationStatus.NAVISENS_INITIALIZED;

    private ZXingScannerView mScannerView;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference dbLat1, dbLon1, dbLat2, dbLon2, user1, user2, trolleyLat1, trolleyLon1, trolleyLat2, trolleyLon2;
    protected boolean is2ndtrolley = false, is1stTrolley = false;

    private Map<String, String> payload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, DemoApplication.MAPBOX_ACCESS_TOKEN);
        setContentView(R.layout.activity_map);
        mScannerView = findViewById(R.id.scanner);
        mScannerView.setVisibility(View.GONE);
        currentLat = findViewById(R.id.currentLat);
        currentLon = findViewById(R.id.currentLon);
        sharedLat = findViewById(R.id.currSharedLat);
        sharedLon = findViewById(R.id.currSharedLon);
        Button camBtn = findViewById(R.id.btn_cam);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        dbLat1 = mFirebaseDatabase.getReference().child("troli1").child("userLat");
        dbLon1 = mFirebaseDatabase.getReference().child("troli1").child("userLon");
        dbLat2 = mFirebaseDatabase.getReference().child("troli2").child("userLat");
        dbLon2 = mFirebaseDatabase.getReference().child("troli2").child("userLon");
        trolleyLat1 = mFirebaseDatabase.getReference().child("troli1").child("currentLat");
        trolleyLon1 = mFirebaseDatabase.getReference().child("troli1").child("currentLon");
        trolleyLat2 = mFirebaseDatabase.getReference().child("troli2").child("currentLat");
        trolleyLon2 = mFirebaseDatabase.getReference().child("troli2").child("currentLon");
        user1 = mFirebaseDatabase.getReference().child("troli1").child("connectedTo");
        user2 = mFirebaseDatabase.getReference().child("troli2").child("connectedTo");
        is1stTrolley = true;
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl(DemoApplication.MAPWIZE_STYLE_URL_BASE + AccountManager.getInstance().getApiKey());
        MapOptions opts = new MapOptions.Builder().build();
        mapwizePlugin = new MapwizePlugin(mapView, opts);
        mapwizePlugin.setOnDidLoadListener(plugin -> {
            mapwizePlugin.setFollowUserMode(FollowUserMode.FOLLOW_USER);
            requestLocationPermission();
            mapwizePlugin.setOnVenueEnterListener(new MapwizePlugin.OnVenueEnterListener() {
                @Override
                public void onVenueEnter(Venue venue) {
                    mapwizePlugin.centerOnVenue(venue);
                }

                @Override
                public void willEnterInVenue(Venue venue) {

                }
            });
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

    protected void getCurrLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        if(navisensIndoorLocationProvider!=null&&navisensIndoorLocationProvider.getLastLocation()!=null){
            double lat = navisensIndoorLocationProvider.getLastLocation().getLatitude();
            double lon = navisensIndoorLocationProvider.getLastLocation().getLongitude();
            currLat = Double.toString(lat);
            currLon = Double.toString(lon);
            currentLat.setText(currLat);
            currentLon.setText(currLon);
            if(!is2ndtrolley&&is1stTrolley) {
                dbLat1.setValue(lat);
                dbLon1.setValue(lon);
            }
            else if(is2ndtrolley&&!is1stTrolley){
                dbLat2.setValue(lat);
                dbLon2.setValue(lon);
            }
        }
    }

    protected void getSharedLocation(){
        if(MotionDna.LocationStatus.NAVISENS_INITIALIZED==locationStat1) {
            try {
                String LatLon = navisensIndoorLocationProvider.sharedLoc;
                String[] parts = LatLon.split(" ");
                getLat = parts[0];
                getLon = parts[1];
                double shareLat = Double.parseDouble(getLat);
                double shareLon = Double.parseDouble(getLon);
                sharedLat.setText(getLat);
                sharedLon.setText(getLon);
                if(is1stTrolley&&!is2ndtrolley){
                    trolleyLat1.setValue(shareLat);
                    trolleyLon1.setValue(shareLon);
                }
                else if(!is1stTrolley&&is2ndtrolley){
                    trolleyLat2.setValue(shareLat);
                    trolleyLon2.setValue(shareLon);
                }
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
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

    boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null) {
            if(!data.getStringExtra("url").equals("Troli2")&&!data.getStringExtra("url").equals("Troli1")) {
                String[] rawLatQR = data.getStringExtra("url").split(",");
                Double latQR = Double.parseDouble(rawLatQR[0]);
                Double lonQR = Double.parseDouble(rawLatQR[1]);
                navisensIndoorLocationProvider.setLocFromQR(latQR, lonQR);
            }
            else if(data.getStringExtra("url").equals("Troli2")){
                is2ndtrolley=true;
                is1stTrolley=false;
                navisensIndoorLocationProvider.start2ndUDP();
            }
            else if(data.getStringExtra("url").equals("Troli1")){
                is1stTrolley=true;
                is2ndtrolley=false;
            }
        }
    }
}
