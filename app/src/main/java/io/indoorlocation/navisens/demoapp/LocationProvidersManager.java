package io.indoorlocation.navisens.demoapp;

import android.app.Activity;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.core.IndoorLocationProviderListener;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.navisens.NavisensIndoorLocationProvider;
import io.mapwize.mapwizeformapbox.AccountManager;
import io.mapwize.mapwizeformapbox.model.Venue;
import okhttp3.OkHttpClient;

public class LocationProvidersManager extends IndoorLocationProvider implements IndoorLocationProviderListener {

    private double MIN_DISTANCE_TO_ACTIVATE = 1000;
    private Activity activity;
    private List<Venue> venues;
    private Venue activeVenue;
    private ManualIndoorLocationProvider manualProvider;
    private NavisensIndoorLocationProvider navisensProvider;
    private boolean started = false;

    public LocationProvidersManager(Activity activity, OkHttpClient client) {
        this.activity = activity;
        this.manualProvider = new ManualIndoorLocationProvider();
    }

    @Override
    public boolean supportsFloor() {
        return true;
    }

    public void start() {
        if (!started) {
            started = true;
            if (mIndoorLocation != null) {
                checkVenueForIndoorLocationActivation();
            }
        }
    }

    public void stop() {
        this.deactivateAll();
        if (this.navisensProvider != null) {
            this.navisensProvider.stop();
            this.navisensProvider = null;
        }

        this.activeVenue = null;
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    public void defineLocation(IndoorLocation indoorLocation) {
        this.manualProvider.setIndoorLocation(indoorLocation);
    }

    public void setVenues(List<Venue> venues) {
        this.venues = venues;
    }

    private void activateNavisens(String navisensKey, Map<Double, Double> floorByAltitude) {
        this.navisensProvider = new NavisensIndoorLocationProvider(this.activity, navisensKey);
        this.navisensProvider.addListener(this);
        this.navisensProvider.start();
    }

    private void deactivateNavisens() {
        if (this.navisensProvider != null) {
            this.navisensProvider.removeListener(this);
            this.navisensProvider.stop();
            this.navisensProvider = null;
        }
    }

    private void deactivateAll() {
        deactivateNavisens();
    }

    private Venue getNearestVenue(IndoorLocation location) {
        if (this.venues == null || this.venues.size() == 0) {
            return null;
        }
        LatLng latLng = new LatLng(location);
        Venue nearestVenue = this.venues.get(0);
        double distanceMin = getDistance(latLng, nearestVenue);
        for (Venue venue : venues) {
            double distance = getDistance(latLng, venue);
            if (distance < distanceMin) {
                nearestVenue = venue;
                distanceMin = distance;
            }
        }

        if (distanceMin < MIN_DISTANCE_TO_ACTIVATE) {
            return nearestVenue;
        }
        return null;
    }

    private double getDistance(LatLng latLng, Venue venue) {
        LatLng venueLatLng = venue.getMarker();
        double R = 6381000;
        double lat1 = latLng.getLatitude() * Math.PI / 180d;
        double lat2 = venueLatLng.getLatitude() * Math.PI / 180d;
        double deltaLat = (venueLatLng.getLatitude() - latLng.getLatitude()) * Math.PI /180d;
        double deltaLng = (venueLatLng.getLongitude() - latLng.getLongitude()) * Math.PI /180d;
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLng/2) * Math.sin(deltaLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private void activateVenue(Venue venue) {
        if (venue != activeVenue) {
            activeVenue = venue;
            JSONObject providers = venue.getIndoorLocationProviders();
            if (providers == null) {
                return;
            }
            JSONObject navisensProviderObject = providers.optJSONObject("navisens");
            if (navisensProviderObject != null) {
                boolean enabled = navisensProviderObject.optBoolean("enabled", false);
                if (enabled) {
                    String apiKey = navisensProviderObject.optString("apiKey");
                    if (apiKey != null) {
                        JSONArray array = navisensProviderObject.optJSONArray("heights");
                        Map<Double,Double> floors = new HashMap<>();
                        if (array != null) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.optJSONObject(i);
                                if (object != null) {
                                    floors.put(object.optDouble("floor"), object.optDouble("height"));
                                }
                            }
                        }
                        activateNavisens(apiKey, floors);
                    }
                }
            }

        }
    }

    public void checkVenueForIndoorLocationActivation() {
        if (mIndoorLocation == null) {
            return;
        }
        final Venue venue = getNearestVenue(mIndoorLocation);
        if (venue != null) {
            if (activeVenue != null && !activeVenue.getId().equals(venue.getId())) {
                deactivateAll();
            }
            if (activeVenue == null || !activeVenue.getId().equals(venue.getId())) {
                activateVenue(venue);
            }
        } else {
            deactivateAll();
        }
    }

    private IndoorLocation mIndoorLocation;
    private IndoorLocationProviderListener gpsIndoorLocationProvider = new IndoorLocationProviderListener() {
        @Override
        public void onProviderStarted() {
            // Don't need
        }

        @Override
        public void onProviderStopped() {
            // Don't need
        }

        @Override
        public void onProviderError(Error error) {
            // Don't need
        }

        @Override
        public void onIndoorLocationChange(IndoorLocation indoorLocation) {
            mIndoorLocation = indoorLocation;
            checkVenueForIndoorLocationActivation();
        }
    };


    @Override
    public void onProviderStarted() {
        dispatchOnProviderStarted();
    }

    @Override
    public void onProviderStopped() {
        dispatchOnProviderStopped();
    }

    @Override
    public void onProviderError(Error error) {
        dispatchOnProviderError(error);
    }

    @Override
    public void onIndoorLocationChange(IndoorLocation indoorLocation) {
        if (indoorLocation != null && indoorLocation.getProvider() != null) {
            MapwizeIndoorLocation mapwizeIndoorLocation = new MapwizeIndoorLocation(indoorLocation);
            if (this.navisensProvider != null) {
                if (mapwizeIndoorLocation.getProvider().equals(this.navisensProvider.getName())) {
                    dispatchIndoorLocationChange(mapwizeIndoorLocation);
                }
                else if (mapwizeIndoorLocation.getFloor() != null) {
                    this.navisensProvider.setIndoorLocation(mapwizeIndoorLocation);
                }
                else {
                    dispatchIndoorLocationChange(mapwizeIndoorLocation);
                }
            }
            else {
                dispatchIndoorLocationChange(mapwizeIndoorLocation);
            }
        }
    }
}
