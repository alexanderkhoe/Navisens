package io.indoorlocation.navisens.demoapp;

import android.location.Location;

import io.indoorlocation.core.IndoorLocation;
import io.mapwize.mapwizeformapbox.model.DirectionPoint;
import io.mapwize.mapwizeformapbox.model.DirectionPointWrapper;

public class MapwizeIndoorLocation extends IndoorLocation implements DirectionPoint {

    public MapwizeIndoorLocation(Location location, Double floor) {
        super(location, floor);
    }

    public MapwizeIndoorLocation(String provider, double latitude, double longitude, Double floor, long timeStamp) {
        super(provider, latitude, longitude, floor, timeStamp);
    }

    public MapwizeIndoorLocation(IndoorLocation indoorLocation) {
        super(indoorLocation.getProvider(), indoorLocation.getLatitude(), indoorLocation.getLongitude(), indoorLocation.getFloor(), indoorLocation.getTime());
    }

    @Override
    public DirectionPointWrapper toDirectionWrapper() {
        return new DirectionPointWrapper.Builder()
                .latitude(this.getLatitude())
                .longitude(this.getLongitude())
                .floor(this.getFloor())
                .build();
    }
}
